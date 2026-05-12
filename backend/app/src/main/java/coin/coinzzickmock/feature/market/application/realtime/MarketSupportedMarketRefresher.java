package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.market.application.gateway.MarketDataGateway;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketSupportedMarketRefresher {
    private final MarketDataGateway marketDataGateway;
    private final MarketSnapshotStore marketSnapshotStore;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MarketFundingScheduleLookup marketFundingScheduleLookup;
    private final RealtimeMarketSummaryProjector realtimeMarketSummaryProjector;

    @Autowired
    public MarketSupportedMarketRefresher(
            MarketDataGateway marketDataGateway,
            MarketSnapshotStore marketSnapshotStore,
            ApplicationEventPublisher applicationEventPublisher,
            MarketFundingScheduleLookup marketFundingScheduleLookup,
            RealtimeMarketSummaryProjector realtimeMarketSummaryProjector
    ) {
        this.marketDataGateway = marketDataGateway;
        this.marketSnapshotStore = marketSnapshotStore;
        this.applicationEventPublisher = applicationEventPublisher;
        this.marketFundingScheduleLookup = marketFundingScheduleLookup;
        this.realtimeMarketSummaryProjector = realtimeMarketSummaryProjector;
    }

    MarketSupportedMarketRefresher(
            MarketDataGateway marketDataGateway,
            MarketSnapshotStore marketSnapshotStore,
            ApplicationEventPublisher applicationEventPublisher,
            MarketFundingScheduleLookup marketFundingScheduleLookup
    ) {
        this(
                marketDataGateway,
                marketSnapshotStore,
                applicationEventPublisher,
                marketFundingScheduleLookup,
                new RealtimeMarketSummaryProjector(new RealtimeMarketDataStore(), marketFundingScheduleLookup)
        );
    }

    public List<MarketSummaryResult> refreshSupportedMarkets() {
        List<MarketSummaryResult> realtimeMarkets = refreshFromRealtimeStore();
        if (!realtimeMarkets.isEmpty()) {
            return realtimeMarkets;
        }

        List<MarketSummaryResult> refreshedMarkets = loadSupportedMarketsFromProvider();
        if (refreshedMarkets.isEmpty()) {
            return List.of();
        }

        updateSupportedMarkets(refreshedMarkets);
        return refreshedMarkets;
    }

    private List<MarketSummaryResult> loadSupportedMarketsFromProvider() {
        return marketDataGateway.loadSupportedMarkets()
                .stream()
                .filter(Objects::nonNull)
                .map(this::toResult)
                .toList();
    }

    private List<MarketSummaryResult> refreshFromRealtimeStore() {
        List<MarketSummaryResult> cachedMarkets = marketSnapshotStore.getSupportedMarkets();
        if (cachedMarkets.isEmpty()) {
            return List.of();
        }

        List<MarketSummaryResult> realtimeMarkets = cachedMarkets.stream()
                .map(cached -> realtimeMarketSummaryProjector.project(cached.symbol(), cached).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        if (realtimeMarkets.isEmpty()) {
            return List.of();
        }

        updateSupportedMarkets(realtimeMarkets);
        return realtimeMarkets;
    }

    private void updateSupportedMarkets(List<MarketSummaryResult> markets) {
        Map<String, MarketSummaryUpdatedEvent> events = marketUpdateEvents(markets);
        marketSnapshotStore.putSupportedMarkets(markets);
        markets.forEach(result -> publishMarketUpdateEvent(result.symbol(), events.get(result.symbol())));
    }

    private void publishMarketUpdateEvent(String symbol, MarketSummaryUpdatedEvent event) {
        try {
            applicationEventPublisher.publishEvent(event);
        } catch (RuntimeException exception) {
            log.warn("Market summary update event publication failed. symbol={}", symbol, exception);
        }
    }

    private Map<String, MarketSummaryUpdatedEvent> marketUpdateEvents(List<MarketSummaryResult> markets) {
        return markets.stream()
                .collect(Collectors.toMap(
                        MarketSummaryResult::symbol,
                        result -> MarketSummaryUpdatedEvent.from(
                                marketSnapshotStore.getMarket(result.symbol()).orElse(null),
                                result
                        ),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
    }

    private MarketSummaryResult toResult(MarketSnapshot snapshot) {
        Instant serverTime = Instant.now();
        FundingSchedule fundingSchedule = marketFundingScheduleLookup.scheduleFor(snapshot.symbol());
        return new MarketSummaryResult(
                snapshot.symbol(),
                snapshot.displayName(),
                snapshot.lastPrice(),
                snapshot.markPrice(),
                snapshot.indexPrice(),
                snapshot.fundingRate(),
                snapshot.change24h(),
                snapshot.turnover24hUsdt(),
                serverTime,
                fundingSchedule.nextFundingAt(serverTime),
                fundingSchedule.intervalHours()
        );
    }
}
