package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "wallet_history")
public class WalletHistoryEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, length = 64)
    private Long memberId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "baseline_wallet_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal baselineWalletBalance;

    @Column(name = "wallet_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal walletBalance;

    @Column(name = "daily_wallet_change", nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyWalletChange;

    @Column(name = "account_version", nullable = false)
    private long accountVersion;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected WalletHistoryEntity() {
    }

    private WalletHistoryEntity(
            Long memberId,
            LocalDate snapshotDate,
            BigDecimal baselineWalletBalance,
            BigDecimal walletBalance,
            BigDecimal dailyWalletChange,
            long accountVersion,
            Instant recordedAt
    ) {
        this.memberId = memberId;
        this.snapshotDate = snapshotDate;
        this.baselineWalletBalance = baselineWalletBalance;
        this.walletBalance = walletBalance;
        this.dailyWalletChange = dailyWalletChange;
        this.accountVersion = accountVersion;
        this.recordedAt = recordedAt;
    }

    public static WalletHistoryEntity baseline(TradingAccount account, LocalDate snapshotDate, Instant recordedAt) {
        return new WalletHistoryEntity(
                account.memberId(),
                snapshotDate,
                BigDecimal.valueOf(account.walletBalance()),
                BigDecimal.valueOf(account.walletBalance()),
                BigDecimal.ZERO,
                -1,
                recordedAt
        );
    }

    public void updateFrom(TradingAccount account, Instant recordedAt) {
        if (account.version() <= accountVersion) {
            return;
        }
        this.walletBalance = BigDecimal.valueOf(account.walletBalance());
        this.dailyWalletChange = walletBalance.subtract(baselineWalletBalance);
        this.accountVersion = account.version();
        this.recordedAt = recordedAt;
    }

    public WalletHistorySnapshot toDomain() {
        return new WalletHistorySnapshot(
                memberId,
                snapshotDate,
                baselineWalletBalance,
                walletBalance,
                dailyWalletChange,
                recordedAt
        );
    }
}
