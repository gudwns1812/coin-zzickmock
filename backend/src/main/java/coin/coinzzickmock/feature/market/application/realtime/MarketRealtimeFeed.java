package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.Providers;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketRealtimeFeed {

    private final Providers providers;
    private final MarketHistoryRecorder marketHistoryRecorder;
    private final MarketHistoryStartupBackfill marketHistoryStartupBackfill;
    private final MarketSnapshotStore marketSnapshotStore;
    private final ApplicationEventPublisher applicationEventPublisher;

    @PostConstruct
    void initializeCache() {
        initializeCache(Instant.now());
    }

    @Scheduled(fixedDelayString = "${coin.market.refresh-delay-ms:1000}")
    public void refreshSupportedMarkets() {
        refreshSupportedMarkets(Instant.now());
    }

    void initializeCache(Instant observedAt) {
        List<MarketSummaryResult> refreshedMarkets = loadSupportedMarkets();
        if (refreshedMarkets.isEmpty()) {
            return;
        }

        marketHistoryStartupBackfill.backfillMissingMinuteHistory(
                refreshedMarkets,
                observedAt,
                providers.connector().marketDataGateway()
        );
        marketSnapshotStore.putSupportedMarkets(refreshedMarkets);
        publishUpdatedMarkets(refreshedMarkets);
        persistHistory(refreshedMarkets, observedAt);
    }

    void refreshSupportedMarkets(Instant observedAt) {
        List<MarketSummaryResult> refreshedMarkets = loadSupportedMarkets();
        if (refreshedMarkets.isEmpty()) {
            return;
        }

        marketSnapshotStore.putSupportedMarkets(refreshedMarkets);
        publishUpdatedMarkets(refreshedMarkets);
        persistHistory(refreshedMarkets, observedAt);
    }

    private List<MarketSummaryResult> loadSupportedMarkets() {
        return providers.connector().marketDataGateway().loadSupportedMarkets()
                .stream()
                .filter(Objects::nonNull)
                .map(this::toResult)
                .toList();
    }

    public List<MarketSummaryResult> getSupportedMarkets() {
        if (!marketSnapshotStore.hasSupportedMarkets()) {
            refreshSupportedMarkets();
        }

        return marketSnapshotStore.getSupportedMarkets();
    }

    public MarketSummaryResult getMarket(String symbol) {
        MarketSummaryResult cached = marketSnapshotStore.getMarket(symbol).orElse(null);
        if (cached != null) {
            return cached;
        }

        return getSupportedMarkets().stream()
                .filter(result -> result.symbol().equals(symbol))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorCode.MARKET_NOT_FOUND));
    }

    private void publishUpdatedMarkets(List<MarketSummaryResult> refreshedMarkets) {
        refreshedMarkets.forEach(
                result -> applicationEventPublisher.publishEvent(new MarketSummaryUpdatedEvent(result)));
    }

    private void persistHistory(List<MarketSummaryResult> refreshedMarkets, Instant observedAt) {
        marketHistoryRecorder.recordSnapshots(refreshedMarkets, observedAt);
    }

    private MarketSummaryResult toResult(MarketSnapshot snapshot) {
        return new MarketSummaryResult(
                snapshot.symbol(),
                snapshot.displayName(),
                snapshot.lastPrice(),
                snapshot.markPrice(),
                snapshot.indexPrice(),
                snapshot.fundingRate(),
                snapshot.change24h()
        );
    }
}
