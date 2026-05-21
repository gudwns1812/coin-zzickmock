package coin.coinzzickmock.feature.market.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "market_symbols")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
}
