package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet_history")
public class WalletHistoryEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, length = 64)
    private Long memberId;

    @Column(name = "wallet_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal walletBalance;

    @Column(name = "available_margin", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableMargin;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_reference", nullable = false, length = 255)
    private String sourceReference;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected WalletHistoryEntity() {
    }

    private WalletHistoryEntity(
            Long memberId,
            BigDecimal walletBalance,
            BigDecimal availableMargin,
            String sourceType,
            String sourceReference,
            Instant recordedAt
    ) {
        this.memberId = memberId;
        this.walletBalance = walletBalance;
        this.availableMargin = availableMargin;
        this.sourceType = sourceType;
        this.sourceReference = sourceReference;
        this.recordedAt = recordedAt;
    }

    public static WalletHistoryEntity from(TradingAccount account, WalletHistorySource source, Instant recordedAt) {
        return new WalletHistoryEntity(
                account.memberId(),
                BigDecimal.valueOf(account.walletBalance()),
                BigDecimal.valueOf(account.availableMargin()),
                source.sourceType(),
                source.sourceReference(),
                recordedAt
        );
    }

    public WalletHistorySnapshot toDomain() {
        return new WalletHistorySnapshot(
                memberId,
                walletBalance.doubleValue(),
                availableMargin.doubleValue(),
                sourceType,
                sourceReference,
                recordedAt
        );
    }
}
