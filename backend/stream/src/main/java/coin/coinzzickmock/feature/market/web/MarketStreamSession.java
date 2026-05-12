package coin.coinzzickmock.feature.market.web;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

final class MarketStreamSession {
    private final MarketStreamSessionKey key;
    private final SseEmitter emitter;
    // Guarded by MarketStreamRegistry's synchronized methods.
    private final Map<String, EnumSet<SummarySubscriptionReason>> summaryReasonsBySymbol = new LinkedHashMap<>();
    private CandleSubscription candleSubscription;

    MarketStreamSession(
            MarketStreamSessionKey key,
            SseEmitter emitter,
            String activeSymbol,
            Set<String> openPositionSymbols,
            CandleSubscription candleSubscription
    ) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.emitter = Objects.requireNonNull(emitter, "emitter must not be null");
        this.candleSubscription = Objects.requireNonNull(candleSubscription, "candleSubscription must not be null");
        Objects.requireNonNull(activeSymbol, "activeSymbol must not be null");
        Objects.requireNonNull(openPositionSymbols, "openPositionSymbols must not be null");
        addSummaryReason(activeSymbol, SummarySubscriptionReason.ACTIVE_SYMBOL);
        for (String openPositionSymbol : openPositionSymbols) {
            Objects.requireNonNull(openPositionSymbol, "openPositionSymbol must not be null");
            addSummaryReason(openPositionSymbol, SummarySubscriptionReason.OPEN_POSITION);
        }
    }

    MarketStreamSessionKey key() {
        return key;
    }

    Long memberId() {
        return key.memberId();
    }

    SseEmitter emitter() {
        return emitter;
    }

    CandleSubscription candleSubscription() {
        return candleSubscription;
    }

    Set<String> summarySymbols() {
        return Set.copyOf(summaryReasonsBySymbol.keySet());
    }

    boolean addSummaryReason(String symbol, SummarySubscriptionReason reason) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        EnumSet<SummarySubscriptionReason> reasons = summaryReasonsBySymbol.computeIfAbsent(
                symbol,
                ignored -> EnumSet.noneOf(SummarySubscriptionReason.class)
        );
        return reasons.add(reason);
    }

    boolean removeSummaryReason(String symbol, SummarySubscriptionReason reason) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        EnumSet<SummarySubscriptionReason> reasons = summaryReasonsBySymbol.get(symbol);
        if (reasons == null || !reasons.remove(reason)) {
            return false;
        }
        if (reasons.isEmpty()) {
            summaryReasonsBySymbol.remove(symbol);
        }
        return true;
    }

    boolean hasSummarySymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        return summaryReasonsBySymbol.containsKey(symbol);
    }

    CandleSubscription replaceCandleSubscription(CandleSubscription next) {
        CandleSubscription previous = this.candleSubscription;
        this.candleSubscription = Objects.requireNonNull(next, "next must not be null");
        return previous;
    }
}
