package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.history.application.implement.FinalizedCandleIntervalsReader;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.web.MarketFinalizedCandleIntervalsReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MarketStreamBridgeConfiguration {
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
}
