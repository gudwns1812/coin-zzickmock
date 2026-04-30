package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.Providers;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class MarketSupportedMarketRefresher {
    private final Providers providers;
    private final MarketSnapshotStore marketSnapshotStore;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MarketFundingScheduleLookup marketFundingScheduleLookup;
    private final RealtimeMarketSummaryProjector realtimeMarketSummaryProjector;

    @Autowired
    public MarketSupportedMarketRefresher(
            Providers providers,
            MarketSnapshotStore marketSnapshotStore,
            ApplicationEventPublisher applicationEventPublisher,
            MarketFundingScheduleLookup marketFundingScheduleLookup,
            RealtimeMarketSummaryProjector realtimeMarketSummaryProjector
    ) {
        this.providers = providers;
        this.marketSnapshotStore = marketSnapshotStore;
        this.applicationEventPublisher = applicationEventPublisher;
        this.marketFundingScheduleLookup = marketFundingScheduleLookup;
        this.realtimeMarketSummaryProjector = realtimeMarketSummaryProjector;
    }

    MarketSupportedMarketRefresher(
            Providers providers,
            MarketSnapshotStore marketSnapshotStore,
            ApplicationEventPublisher applicationEventPublisher,
            MarketFundingScheduleLookup marketFundingScheduleLookup
    ) {
        this(
                providers,
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

        List<MarketSummaryResult> refreshedMarkets = providers.connector().marketDataGateway().loadSupportedMarkets()
                .stream()
                .filter(Objects::nonNull)
                .map(this::toResult)
                .toList();
        if (refreshedMarkets.isEmpty()) {
            return List.of();
        }

        Map<String, MarketSummaryUpdatedEvent> events = refreshedMarkets.stream()
                .collect(Collectors.toMap(
                        MarketSummaryResult::symbol,
                        result -> MarketSummaryUpdatedEvent.from(
                                marketSnapshotStore.getMarket(result.symbol()).orElse(null),
                                result
                        ),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
        marketSnapshotStore.putSupportedMarkets(refreshedMarkets);
        refreshedMarkets.forEach(result -> applicationEventPublisher.publishEvent(events.get(result.symbol())));
        return refreshedMarkets;
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

        Map<String, MarketSummaryUpdatedEvent> events = realtimeMarkets.stream()
                .collect(Collectors.toMap(
                        MarketSummaryResult::symbol,
                        result -> MarketSummaryUpdatedEvent.from(
                                marketSnapshotStore.getMarket(result.symbol()).orElse(null),
                                result
                        ),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
        marketSnapshotStore.putSupportedMarkets(realtimeMarkets);
        realtimeMarkets.forEach(result -> applicationEventPublisher.publishEvent(events.get(result.symbol())));
        return realtimeMarkets;
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
