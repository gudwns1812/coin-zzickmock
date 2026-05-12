package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.web.SseSubscriptionLimitExceededException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class MarketStreamRegistry {
    private final int maxSessionsPerSummarySymbol;
    private final int maxSessionsTotal;
    private final Map<MarketStreamSessionKey, MarketStreamSession> sessions = new LinkedHashMap<>();
    private final Map<Long, Set<MarketStreamSessionKey>> memberIndex = new LinkedHashMap<>();
    private final Map<String, Set<MarketStreamSessionKey>> summaryIndex = new LinkedHashMap<>();
    private final Map<CandleSubscription, Set<MarketStreamSessionKey>> candleIndex = new LinkedHashMap<>();

    public MarketStreamRegistry(
            @Value("${coin.market.sse.max-subscribers-per-symbol:50}") int maxSessionsPerSummarySymbol,
            @Value("${coin.market.sse.max-total-subscribers:100}") int maxSessionsTotal
    ) {
        this.maxSessionsPerSummarySymbol = maxSessionsPerSummarySymbol;
        this.maxSessionsTotal = maxSessionsTotal;
    }

    MarketStreamRegistry() {
        this(50, 100);
    }

    public synchronized Registration registerSession(
            MarketStreamSessionKey sessionKey,
            SseEmitter emitter,
            String activeSymbol,
            Set<String> openPositionSymbols,
            CandleSubscription candleSubscription
    ) {
        Objects.requireNonNull(sessionKey, "sessionKey must not be null");
        Objects.requireNonNull(emitter, "emitter must not be null");
        Objects.requireNonNull(candleSubscription, "candleSubscription must not be null");
        Set<String> requestedSummarySymbols = new LinkedHashSet<>();
        requestedSummarySymbols.add(activeSymbol);
        requestedSummarySymbols.addAll(openPositionSymbols);

        MarketStreamSession previous = sessions.get(sessionKey);
        if (previous == null && sessions.size() >= maxSessionsTotal) {
            throw new SseSubscriptionLimitExceededException("total_limit");
        }
        assertSummaryCapacity(sessionKey, requestedSummarySymbols);

        if (previous != null) {
            removeIndexes(previous);
        }

        MarketStreamSession next = new MarketStreamSession(
                sessionKey,
                emitter,
                activeSymbol,
                new LinkedHashSet<>(openPositionSymbols),
                candleSubscription
        );
        sessions.put(sessionKey, next);
        addIndexes(next);
        return new Registration(previous == null ? null : previous.emitter());
    }

    public synchronized boolean releaseSession(MarketStreamSessionKey sessionKey, String reason) {
        MarketStreamSession removed = sessions.remove(sessionKey);
        if (removed == null) {
            return false;
        }
        removeIndexes(removed);
        return true;
    }

    public synchronized boolean releaseSession(
            MarketStreamSessionKey sessionKey,
            SseEmitter emitter,
            String reason
    ) {
        MarketStreamSession current = sessions.get(sessionKey);
        if (current == null || current.emitter() != emitter) {
            return false;
        }
        sessions.remove(sessionKey);
        removeIndexes(current);
        return true;
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
        if (!session.hasSummarySymbol(symbol)) {
            assertSummaryCapacity(sessionKey, Set.of(symbol));
        }
        boolean changed = session.addSummaryReason(symbol, reason);
        if (changed) {
            summaryIndex.computeIfAbsent(symbol, ignored -> new LinkedHashSet<>()).add(sessionKey);
        }
        return changed;
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
        boolean hadSymbol = session.hasSummarySymbol(symbol);
        boolean changed = session.removeSummaryReason(symbol, reason);
        if (changed && hadSymbol && !session.hasSummarySymbol(symbol)) {
            removeFromIndex(summaryIndex, symbol, sessionKey);
        }
        return changed;
    }

    public synchronized boolean replaceCandleSubscription(
            MarketStreamSessionKey sessionKey,
            CandleSubscription nextSubscription
    ) {
        MarketStreamSession session = sessions.get(sessionKey);
        if (session == null) {
            return false;
        }
        CandleSubscription previous = session.replaceCandleSubscription(nextSubscription);
        removeFromIndex(candleIndex, previous, sessionKey);
        candleIndex.computeIfAbsent(nextSubscription, ignored -> new LinkedHashSet<>()).add(sessionKey);
        return true;
    }

    synchronized List<MarketStreamSubscriber> sessionsForSummary(String symbol) {
        return subscribers(summaryIndex.get(symbol));
    }

    synchronized List<MarketStreamSubscriber> sessionsForCandle(CandleSubscription subscription) {
        return subscribers(candleIndex.get(subscription));
    }

    synchronized List<CandleSubscription> candleSubscriptionsForSymbol(String symbol) {
        return candleIndex.keySet().stream()
                .filter(subscription -> subscription.symbol().equals(symbol))
                .toList();
    }

    synchronized List<MarketStreamSessionKey> sessionKeysForMember(Long memberId) {
        Set<MarketStreamSessionKey> keys = memberIndex.get(memberId);
        if (keys == null) {
            return List.of();
        }
        return List.copyOf(keys);
    }

    synchronized int activeSessionCount() {
        return sessions.size();
    }

    synchronized int summarySubscriberCount(String symbol) {
        Set<MarketStreamSessionKey> keys = summaryIndex.get(symbol);
        return keys == null ? 0 : keys.size();
    }

    synchronized int candleSubscriberCount(CandleSubscription subscription) {
        Set<MarketStreamSessionKey> keys = candleIndex.get(subscription);
        return keys == null ? 0 : keys.size();
    }

    private void assertSummaryCapacity(MarketStreamSessionKey sessionKey, Set<String> requestedSummarySymbols) {
        for (String symbol : requestedSummarySymbols) {
            Set<MarketStreamSessionKey> keys = summaryIndex.get(symbol);
            if (keys != null && !keys.contains(sessionKey) && keys.size() >= maxSessionsPerSummarySymbol) {
                throw new SseSubscriptionLimitExceededException("symbol_limit");
            }
        }
    }

    private List<MarketStreamSubscriber> subscribers(Set<MarketStreamSessionKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<MarketStreamSubscriber> subscribers = new ArrayList<>();
        for (MarketStreamSessionKey key : keys) {
            MarketStreamSession session = sessions.get(key);
            if (session != null) {
                subscribers.add(new MarketStreamSubscriber(key, session.emitter()));
            }
        }
        return List.copyOf(subscribers);
    }

    private void addIndexes(MarketStreamSession session) {
        if (session.memberId() != null) {
            memberIndex.computeIfAbsent(session.memberId(), ignored -> new LinkedHashSet<>()).add(session.key());
        }
        for (String symbol : session.summarySymbols()) {
            summaryIndex.computeIfAbsent(symbol, ignored -> new LinkedHashSet<>()).add(session.key());
        }
        candleIndex.computeIfAbsent(session.candleSubscription(), ignored -> new LinkedHashSet<>()).add(session.key());
    }

    private void removeIndexes(MarketStreamSession session) {
        if (session.memberId() != null) {
            removeFromIndex(memberIndex, session.memberId(), session.key());
        }
        for (String symbol : session.summarySymbols()) {
            removeFromIndex(summaryIndex, symbol, session.key());
        }
        removeFromIndex(candleIndex, session.candleSubscription(), session.key());
    }

    private static <K> void removeFromIndex(
            Map<K, Set<MarketStreamSessionKey>> index,
            K indexKey,
            MarketStreamSessionKey sessionKey
    ) {
        Set<MarketStreamSessionKey> keys = index.get(indexKey);
        if (keys == null) {
            return;
        }
        keys.remove(sessionKey);
        if (keys.isEmpty()) {
            index.remove(indexKey);
        }
    }

    public record Registration(SseEmitter replacedEmitter) {
    }
}
