package coin.coinzzickmock.feature.market.web;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class MarketStreamRegistry {
    private final Map<MarketStreamSessionKey, MarketStreamSession> sessions = new HashMap<>();
    private final Map<Long, Set<MarketStreamSessionKey>> memberIndex = new HashMap<>();
    private final Map<String, Set<MarketStreamSessionKey>> summaryIndex = new HashMap<>();
    private final Map<CandleSubscription, Set<MarketStreamSessionKey>> candleIndex = new HashMap<>();

    public synchronized Registration registerSession(
            MarketStreamSessionKey sessionKey,
            SseEmitter emitter,
            String activeSymbol,
            Set<String> openPositionSymbols,
            CandleSubscription candleSubscription
    ) {
        MarketStreamSession previous = sessions.remove(sessionKey);
        if (previous != null) {
            removeIndexes(previous);
        }

        Map<String, Set<SummarySubscriptionReason>> reasons = new HashMap<>();
        reasons.computeIfAbsent(normalize(activeSymbol), ignored -> EnumSet.noneOf(SummarySubscriptionReason.class))
                .add(SummarySubscriptionReason.ACTIVE_SYMBOL);
        for (String symbol : openPositionSymbols) {
            reasons.computeIfAbsent(normalize(symbol), ignored -> EnumSet.noneOf(SummarySubscriptionReason.class))
                    .add(SummarySubscriptionReason.OPEN_POSITION);
        }

        MarketStreamSession session = new MarketStreamSession(sessionKey, emitter, reasons, candleSubscription);
        sessions.put(sessionKey, session);
        addIndexes(session);
        return new Registration(session, previous == null ? null : previous.emitter());
    }

    public synchronized SseEmitter releaseSession(MarketStreamSessionKey sessionKey, String reason) {
        MarketStreamSession removed = sessions.remove(sessionKey);
        if (removed == null) {
            return null;
        }
        removeIndexes(removed);
        return removed.emitter();
    }

    public synchronized boolean addSummaryReason(
            MarketStreamSessionKey sessionKey,
            String symbol,
            SummarySubscriptionReason reason
    ) {
        MarketStreamSession session = sessions.get(sessionKey);
        if (session == null) {
            return false;
        }
        boolean added = session.addSummaryReason(symbol, reason);
        if (added) {
            summaryIndex.computeIfAbsent(normalize(symbol), ignored -> new LinkedHashSet<>()).add(sessionKey);
        }
        return added;
    }

    public synchronized boolean removeSummaryReason(
            MarketStreamSessionKey sessionKey,
            String symbol,
            SummarySubscriptionReason reason
    ) {
        MarketStreamSession session = sessions.get(sessionKey);
        if (session == null) {
            return false;
        }
        boolean removed = session.removeSummaryReason(symbol, reason);
        if (removed && !session.hasSummaryReasons(symbol)) {
            removeIndexEntry(summaryIndex, normalize(symbol), sessionKey);
        }
        return removed;
    }

    public synchronized boolean replaceCandleSubscription(
            MarketStreamSessionKey sessionKey,
            CandleSubscription nextSubscription
    ) {
        MarketStreamSession session = sessions.get(sessionKey);
        if (session == null) {
            return false;
        }
        removeIndexEntry(candleIndex, session.candleSubscription(), sessionKey);
        session.replaceCandleSubscription(nextSubscription);
        candleIndex.computeIfAbsent(nextSubscription, ignored -> new LinkedHashSet<>()).add(sessionKey);
        return true;
    }

    public synchronized List<MarketStreamSession> sessionsForSummary(String symbol) {
        return sessionsFor(summaryIndex.getOrDefault(normalize(symbol), Set.of()));
    }

    public synchronized List<MarketStreamSession> sessionsForCandle(CandleSubscription subscription) {
        return sessionsFor(candleIndex.getOrDefault(subscription, Set.of()));
    }

    public synchronized List<MarketStreamSession> sessionsForMember(Long memberId) {
        return sessionsFor(memberIndex.getOrDefault(memberId, Set.of()));
    }

    public synchronized List<MarketStreamSession> sessionsForCandleSymbol(String symbol) {
        String normalizedSymbol = normalize(symbol);
        return sessions.values()
                .stream()
                .filter(session -> session.candleSubscription().symbol().equals(normalizedSymbol))
                .toList();
    }

    public synchronized int sessionCount() {
        return sessions.size();
    }

    private List<MarketStreamSession> sessionsFor(Set<MarketStreamSessionKey> keys) {
        return keys.stream()
                .map(sessions::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private void addIndexes(MarketStreamSession session) {
        memberIndex.computeIfAbsent(session.memberId(), ignored -> new LinkedHashSet<>()).add(session.key());
        for (String symbol : session.summarySymbols()) {
            summaryIndex.computeIfAbsent(symbol, ignored -> new LinkedHashSet<>()).add(session.key());
        }
        candleIndex.computeIfAbsent(session.candleSubscription(), ignored -> new LinkedHashSet<>()).add(session.key());
    }

    private void removeIndexes(MarketStreamSession session) {
        removeIndexEntry(memberIndex, session.memberId(), session.key());
        for (String symbol : new HashSet<>(session.summarySymbols())) {
            removeIndexEntry(summaryIndex, symbol, session.key());
        }
        removeIndexEntry(candleIndex, session.candleSubscription(), session.key());
    }

    private static <K> void removeIndexEntry(Map<K, Set<MarketStreamSessionKey>> index, K key, MarketStreamSessionKey sessionKey) {
        Set<MarketStreamSessionKey> keys = index.get(key);
        if (keys == null) {
            return;
        }
        keys.remove(sessionKey);
        if (keys.isEmpty()) {
            index.remove(key);
        }
    }

    private static String normalize(String symbol) {
        return symbol.toUpperCase();
    }

    public record Registration(MarketStreamSession session, SseEmitter replacedEmitter) {
    }
}
