package coin.coinzzickmock.feature.market.web;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class MarketStreamSession {
    private final MarketStreamSessionKey key;
    private final SseEmitter emitter;
    private final Map<String, EnumSet<SummarySubscriptionReason>> summaryReasonsBySymbol;
    private CandleSubscription candleSubscription;

    MarketStreamSession(
            MarketStreamSessionKey key,
            SseEmitter emitter,
            Map<String, Set<SummarySubscriptionReason>> summaryReasonsBySymbol,
            CandleSubscription candleSubscription
    ) {
        this.key = key;
        this.emitter = emitter;
        this.summaryReasonsBySymbol = new HashMap<>();
        summaryReasonsBySymbol.forEach((symbol, reasons) -> this.summaryReasonsBySymbol.put(
                normalize(symbol),
                reasons.isEmpty()
                        ? EnumSet.noneOf(SummarySubscriptionReason.class)
                        : EnumSet.copyOf(reasons)
        ));
        this.candleSubscription = candleSubscription;
    }

    public MarketStreamSessionKey key() {
        return key;
    }

    public Long memberId() {
        return key.memberId();
    }

    public SseEmitter emitter() {
        return emitter;
    }

    public CandleSubscription candleSubscription() {
        return candleSubscription;
    }

    void replaceCandleSubscription(CandleSubscription next) {
        this.candleSubscription = next;
    }

    boolean addSummaryReason(String symbol, SummarySubscriptionReason reason) {
        return summaryReasonsBySymbol
                .computeIfAbsent(normalize(symbol), ignored -> EnumSet.noneOf(SummarySubscriptionReason.class))
                .add(reason);
    }

    boolean removeSummaryReason(String symbol, SummarySubscriptionReason reason) {
        String normalizedSymbol = normalize(symbol);
        EnumSet<SummarySubscriptionReason> reasons = summaryReasonsBySymbol.get(normalizedSymbol);
        if (reasons == null || !reasons.remove(reason)) {
            return false;
        }
        if (reasons.isEmpty()) {
            summaryReasonsBySymbol.remove(normalizedSymbol);
        }
        return true;
    }

    boolean hasSummaryReasons(String symbol) {
        EnumSet<SummarySubscriptionReason> reasons = summaryReasonsBySymbol.get(normalize(symbol));
        return reasons != null && !reasons.isEmpty();
    }

    Set<String> summarySymbols() {
        return Set.copyOf(summaryReasonsBySymbol.keySet());
    }

    public Map<String, Set<SummarySubscriptionReason>> summaryReasonsBySymbol() {
        Map<String, Set<SummarySubscriptionReason>> snapshot = new HashMap<>();
        summaryReasonsBySymbol.forEach((symbol, reasons) -> snapshot.put(symbol, Collections.unmodifiableSet(EnumSet.copyOf(reasons))));
        return Collections.unmodifiableMap(snapshot);
    }

    private static String normalize(String symbol) {
        return symbol.toUpperCase();
    }
}
