package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "market_completed_candles")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketCompletedCandleEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol_id", nullable = false)
    private Long symbolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "candle_interval", nullable = false, length = 30)
    private MarketCandleInterval interval;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "open_time", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant openTime;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "close_time", nullable = false, columnDefinition = "DATETIME(6)")
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

    private MarketCompletedCandleEntity(CompletedMarketCandle candle) {
        apply(candle);
    }

    public static MarketCompletedCandleEntity from(CompletedMarketCandle candle) {
        return new MarketCompletedCandleEntity(candle);
    }

    public void apply(CompletedMarketCandle candle) {
        this.symbolId = candle.symbolId();
        this.interval = candle.interval();
        this.openTime = candle.openTime();
        this.closeTime = candle.closeTime();
        this.openPrice = decimal(candle.openPrice());
        this.highPrice = decimal(candle.highPrice());
        this.lowPrice = decimal(candle.lowPrice());
        this.closePrice = decimal(candle.closePrice());
        this.volume = decimal(candle.volume());
        this.quoteVolume = decimal(candle.quoteVolume());
    }

    public CompletedMarketCandle toDomain() {
        return new CompletedMarketCandle(
                symbolId,
                interval,
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

    public HourlyMarketCandle toHourlyMarketCandle() {
        return toDomain().toHourlyMarketCandle();
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }

}
