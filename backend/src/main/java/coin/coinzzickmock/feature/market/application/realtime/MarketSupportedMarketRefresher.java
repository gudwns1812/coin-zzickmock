package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.Providers;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketSupportedMarketRefresher {
    private final Providers providers;
    private final MarketSnapshotStore marketSnapshotStore;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MarketFundingScheduleLookup marketFundingScheduleLookup;

    public List<MarketSummaryResult> refreshSupportedMarkets() {
        List<MarketSummaryResult> refreshedMarkets = providers.connector().marketDataGateway().loadSupportedMarkets()
                .stream()
                .filter(Objects::nonNull)
                .map(this::toResult)
                .toList();
        if (refreshedMarkets.isEmpty()) {
            return List.of();
        }

        marketSnapshotStore.putSupportedMarkets(refreshedMarkets);
        refreshedMarkets.forEach(result -> applicationEventPublisher.publishEvent(new MarketSummaryUpdatedEvent(result)));
        return refreshedMarkets;
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
                serverTime,
                fundingSchedule.nextFundingAt(serverTime),
                fundingSchedule.intervalHours()
        );
    }
}
