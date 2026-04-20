package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "market_candles_1m")
public class MarketCandle1mEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol_id", nullable = false)
    private Long symbolId;

    @Column(name = "open_time", nullable = false)
    private Instant openTime;

    @Column(name = "close_time", nullable = false)
    private Instant closeTime;

    @Column(name = "open_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal closePrice;

    @Column(name = "volume", nullable = false, precision = 19, scale = 8)
    private BigDecimal volume;

    @Column(name = "quote_volume", precision = 19, scale = 4)
    private BigDecimal quoteVolume;

    protected MarketCandle1mEntity() {
    }

    private MarketCandle1mEntity(MarketHistoryCandle candle) {
        apply(candle);
    }

    public static MarketCandle1mEntity from(MarketHistoryCandle candle) {
        return new MarketCandle1mEntity(candle);
    }

    public void apply(MarketHistoryCandle candle) {
        this.symbolId = candle.symbolId();
        this.openTime = candle.openTime();
        this.closeTime = candle.closeTime();
        this.openPrice = decimal(candle.openPrice());
        this.highPrice = decimal(candle.highPrice());
        this.lowPrice = decimal(candle.lowPrice());
        this.closePrice = decimal(candle.closePrice());
        this.volume = decimal(candle.volume());
        this.quoteVolume = decimal(candle.quoteVolume());
    }

    public MarketHistoryCandle toDomain() {
        return new MarketHistoryCandle(
                symbolId,
                openTime,
                closeTime,
                openPrice.doubleValue(),
                highPrice.doubleValue(),
                lowPrice.doubleValue(),
                closePrice.doubleValue(),
                volume.doubleValue(),
                quoteVolume == null ? 0.0 : quoteVolume.doubleValue()
        );
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
