package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.application.query.FinalizedCandleIntervalsReader;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.web.MarketCandleSnapshotReader;
import coin.coinzzickmock.feature.market.web.MarketCurrentCandleBootstrapper;
import coin.coinzzickmock.feature.market.web.MarketFinalizedCandleIntervalsReader;
import coin.coinzzickmock.feature.market.web.MarketOpenPositionSymbolsReader;
import coin.coinzzickmock.feature.market.web.MarketStreamActorReader;
import coin.coinzzickmock.feature.market.web.MarketSummarySnapshotReader;
import coin.coinzzickmock.feature.position.application.query.OpenPositionSymbolsReader;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import java.util.LinkedHashSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MarketStreamBridgeConfiguration {
    @Bean
    MarketSummarySnapshotReader marketSummarySnapshotReader(GetMarketSummaryService getMarketSummaryService) {
        return symbol -> MarketStreamPayloadMapper.toResponse(
                getMarketSummaryService.getMarket(new GetMarketQuery(symbol))
        );
    }

    @Bean
    MarketCandleSnapshotReader marketCandleSnapshotReader(RealtimeMarketCandleProjector realtimeMarketCandleProjector) {
        return (symbol, interval) -> realtimeMarketCandleProjector.latest(symbol, MarketCandleInterval.from(interval))
                .map(MarketStreamPayloadMapper::toResponse);
    }

    @Bean
    MarketCurrentCandleBootstrapper marketCurrentCandleBootstrapper(
            coin.coinzzickmock.feature.market.application.realtime.CurrentMarketCandleBootstrapper bootstrapper
    ) {
        return (symbol, interval) -> bootstrapper.bootstrapIfNeeded(symbol, MarketCandleInterval.from(interval));
    }

    @Bean
    MarketFinalizedCandleIntervalsReader marketFinalizedCandleIntervalsReader(
            FinalizedCandleIntervalsReader finalizedCandleIntervalsReader
    ) {
        return (symbol, openTime, closeTime) -> finalizedCandleIntervalsReader
                .readAffectedIntervals(symbol, openTime, closeTime)
                .stream()
                .map(MarketCandleInterval::value)
                .toList();
    }

    @Bean
    MarketOpenPositionSymbolsReader marketOpenPositionSymbolsReader(OpenPositionSymbolsReader openPositionSymbolsReader) {
        return memberId -> new LinkedHashSet<>(openPositionSymbolsReader.openSymbols(memberId));
    }

    @Bean
    MarketStreamActorReader marketStreamActorReader(Providers providers) {
        return () -> providers.auth().currentActorOptional().map(Actor::memberId);
    }
}
