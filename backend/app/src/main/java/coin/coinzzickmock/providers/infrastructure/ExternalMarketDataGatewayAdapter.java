package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExternalMarketDataGatewayAdapter implements MarketDataGateway {
    private final coin.coinzzickmock.providers.connector.MarketDataGateway delegate;

    public ExternalMarketDataGatewayAdapter(coin.coinzzickmock.providers.connector.MarketDataGateway delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<MarketSnapshot> loadSupportedMarkets() {
        try {
            return delegate.loadSupportedMarkets().stream()
                    .map(this::toDomain)
                    .toList();
        } catch (RuntimeException exception) {
            throw translate("load supported markets", null, null, null, null, exception);
        }
    }

    @Override
    public MarketSnapshot loadMarket(String symbol) {
        try {
            return toDomain(delegate.loadMarket(symbol));
        } catch (RuntimeException exception) {
            throw translate("load market", symbol, null, null, null, exception);
        }
    }

    @Override
    public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
            String symbol,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        try {
            return delegate.loadMinuteCandles(symbol, fromInclusive, toExclusive).stream()
                    .map(this::toDomain)
                    .toList();
        } catch (RuntimeException exception) {
            throw translate("load minute candles", symbol, null, fromInclusive, toExclusive, exception);
        }
    }

    @Override
    public List<MarketHistoricalCandleSnapshot> loadHistoricalCandles(
            String symbol,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int limit
    ) {
        try {
            return delegate.loadHistoricalCandles(symbol, toProvider(interval), fromInclusive, toExclusive, limit)
                    .stream()
                    .map(this::toDomain)
                    .toList();
        } catch (RuntimeException exception) {
            throw translate("load historical candles", symbol, interval, fromInclusive, toExclusive, exception);
        }
    }

    private MarketSnapshot toDomain(ProviderMarketSnapshot snapshot) {
        return new MarketSnapshot(
                snapshot.symbol(),
                snapshot.displayName(),
                snapshot.lastPrice().doubleValue(),
                snapshot.markPrice().doubleValue(),
                snapshot.indexPrice().doubleValue(),
                snapshot.fundingRate().doubleValue(),
                snapshot.change24h().doubleValue(),
                snapshot.turnover24hUsdt().doubleValue()
        );
    }

    private MarketMinuteCandleSnapshot toDomain(ProviderMarketMinuteCandleSnapshot candle) {
        return new MarketMinuteCandleSnapshot(
                candle.openTime(),
                candle.closeTime(),
                candle.openPrice().doubleValue(),
                candle.highPrice().doubleValue(),
                candle.lowPrice().doubleValue(),
                candle.closePrice().doubleValue(),
                candle.volume().doubleValue(),
                candle.quoteVolume().doubleValue()
        );
    }

    private MarketHistoricalCandleSnapshot toDomain(ProviderMarketHistoricalCandleSnapshot candle) {
        return new MarketHistoricalCandleSnapshot(
                candle.openTime(),
                candle.closeTime(),
                candle.openPrice().doubleValue(),
                candle.highPrice().doubleValue(),
                candle.lowPrice().doubleValue(),
                candle.closePrice().doubleValue(),
                candle.volume().doubleValue(),
                candle.quoteVolume().doubleValue()
        );
    }

    private CoreException translate(
            String operation,
            String symbol,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            RuntimeException exception
    ) {
        log.warn(
                "External market gateway failed. operation={} symbol={} interval={} from={} to={}",
                operation,
                symbol,
                interval == null ? null : interval.value(),
                fromInclusive,
                toExclusive,
                exception
        );
        return new CoreException(ErrorCode.INTERNAL_SERVER_ERROR);
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
