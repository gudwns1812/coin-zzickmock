package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.feature.market.application.gateway.MarketDataGateway;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import coin.coinzzickmock.providers.connector.ProviderMarketHistoricalCandleSnapshot;
import coin.coinzzickmock.providers.connector.ProviderMarketMinuteCandleSnapshot;
import coin.coinzzickmock.providers.connector.ProviderMarketSnapshot;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExternalMarketDataGatewayAdapter implements MarketDataGateway {
    private final coin.coinzzickmock.providers.connector.MarketDataGateway delegate;

    public ExternalMarketDataGatewayAdapter(coin.coinzzickmock.providers.connector.MarketDataGateway delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<MarketSnapshot> loadSupportedMarkets() {
        return delegate.loadSupportedMarkets().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public MarketSnapshot loadMarket(String symbol) {
        return toDomain(delegate.loadMarket(symbol));
    }

    @Override
    public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
            String symbol,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return delegate.loadMinuteCandles(symbol, fromInclusive, toExclusive).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<MarketHistoricalCandleSnapshot> loadHistoricalCandles(
            String symbol,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int limit
    ) {
        return delegate.loadHistoricalCandles(symbol, toProvider(interval), fromInclusive, toExclusive, limit).stream()
                .map(this::toDomain)
                .toList();
    }

    private MarketSnapshot toDomain(ProviderMarketSnapshot snapshot) {
        return new MarketSnapshot(
                snapshot.symbol(),
                snapshot.displayName(),
                snapshot.lastPrice(),
                snapshot.markPrice(),
                snapshot.indexPrice(),
                snapshot.fundingRate(),
                snapshot.change24h(),
                snapshot.turnover24hUsdt()
        );
    }

    private MarketMinuteCandleSnapshot toDomain(ProviderMarketMinuteCandleSnapshot candle) {
        return new MarketMinuteCandleSnapshot(
                candle.openTime(),
                candle.closeTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice(),
                candle.volume(),
                candle.quoteVolume()
        );
    }

    private MarketHistoricalCandleSnapshot toDomain(ProviderMarketHistoricalCandleSnapshot candle) {
        return new MarketHistoricalCandleSnapshot(
                candle.openTime(),
                candle.closeTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice(),
                candle.volume(),
                candle.quoteVolume()
        );
    }

    private ProviderMarketCandleInterval toProvider(MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> ProviderMarketCandleInterval.ONE_MINUTE;
            case THREE_MINUTES -> ProviderMarketCandleInterval.THREE_MINUTES;
            case FIVE_MINUTES -> ProviderMarketCandleInterval.FIVE_MINUTES;
            case FIFTEEN_MINUTES -> ProviderMarketCandleInterval.FIFTEEN_MINUTES;
            case ONE_HOUR -> ProviderMarketCandleInterval.ONE_HOUR;
            case FOUR_HOURS -> ProviderMarketCandleInterval.FOUR_HOURS;
            case TWELVE_HOURS -> ProviderMarketCandleInterval.TWELVE_HOURS;
            case ONE_DAY -> ProviderMarketCandleInterval.ONE_DAY;
            case ONE_WEEK -> ProviderMarketCandleInterval.ONE_WEEK;
            case ONE_MONTH -> ProviderMarketCandleInterval.ONE_MONTH;
        };
    }
}
