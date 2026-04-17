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

    protected MarketSymbolEntity() {
    }

    public Long id() {
        return id;
    }

    public String symbol() {
        return symbol;
    }
}
