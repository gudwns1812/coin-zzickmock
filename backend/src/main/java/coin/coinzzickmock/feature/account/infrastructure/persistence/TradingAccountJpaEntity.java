package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trading_accounts")
public class TradingAccountJpaEntity {
    @Id
    @Column(name = "member_id", nullable = false, length = 64)
    private String memberId;

    @Column(name = "member_email", nullable = false, length = 255)
    private String memberEmail;

    @Column(name = "member_name", nullable = false, length = 100)
    private String memberName;

    @Column(name = "wallet_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal walletBalance;

    @Column(name = "available_margin", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableMargin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TradingAccountJpaEntity() {
    }

    public TradingAccountJpaEntity(
            String memberId,
            String memberEmail,
            String memberName,
            BigDecimal walletBalance,
            BigDecimal availableMargin
    ) {
        this.memberId = memberId;
        this.memberEmail = memberEmail;
        this.memberName = memberName;
        this.walletBalance = walletBalance;
        this.availableMargin = availableMargin;
    }

    public static TradingAccountJpaEntity from(String memberEmail, TradingAccount account) {
        return new TradingAccountJpaEntity(
                account.memberId(),
                memberEmail,
                account.memberName(),
                decimal(account.walletBalance()),
                decimal(account.availableMargin())
        );
    }

    public void apply(String memberEmail, TradingAccount account) {
        this.memberEmail = memberEmail;
        this.memberName = account.memberName();
        this.walletBalance = decimal(account.walletBalance());
        this.availableMargin = decimal(account.availableMargin());
    }

    public TradingAccount toDomain() {
        return new TradingAccount(
                memberId,
                memberName,
                walletBalance.doubleValue(),
                availableMargin.doubleValue()
        );
    }

    public String memberId() {
        return memberId;
    }

    public String memberEmail() {
        return memberEmail;
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
