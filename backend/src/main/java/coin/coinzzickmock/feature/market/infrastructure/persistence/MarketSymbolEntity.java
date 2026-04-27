package coin.coinzzickmock.feature.market.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "market_symbols")
public class MarketSymbolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 30)
    private String symbol;

    @Column(name = "funding_interval_hours", nullable = false)
    private int fundingIntervalHours;

    @Column(name = "funding_anchor_hour", nullable = false)
    private int fundingAnchorHour;

    @Column(name = "funding_time_zone", nullable = false, length = 40)
    private String fundingTimeZone;

    protected MarketSymbolEntity() {
    }

    public Long id() {
        return id;
    }

    public String symbol() {
        return symbol;
    }

    public int fundingIntervalHours() {
        return fundingIntervalHours;
    }

    public int fundingAnchorHour() {
        return fundingAnchorHour;
    }

    public String fundingTimeZone() {
        return fundingTimeZone;
    }
}
