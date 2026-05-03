package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "trading_accounts")
public class TradingAccountEntity extends AuditableEntity {
    @Id
    @Column(name = "member_id", nullable = false, length = 64)
    private Long memberId;

    @Column(name = "member_email", nullable = false, length = 255)
    private String memberEmail;

    @Column(name = "member_name", nullable = false, length = 100)
    private String memberName;

    @Column(name = "wallet_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal walletBalance;

    @Column(name = "available_margin", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableMargin;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TradingAccountEntity() {
    }

    public TradingAccountEntity(
            Long memberId,
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

    public static TradingAccountEntity from(TradingAccount account) {
        return new TradingAccountEntity(
                account.memberId(),
                account.memberEmail(),
                account.memberName(),
                decimal(account.walletBalance()),
                decimal(account.availableMargin())
        );
    }

    public void apply(TradingAccount account) {
        this.memberEmail = account.memberEmail();
        this.memberName = account.memberName();
        this.walletBalance = decimal(account.walletBalance());
        this.availableMargin = decimal(account.availableMargin());
    }

    public TradingAccount toDomain() {
        return new TradingAccount(
                memberId,
                memberEmail,
                memberName,
                walletBalance.doubleValue(),
                availableMargin.doubleValue(),
                version
        );
    }

    public Long memberId() {
        return memberId;
    }

    public String memberEmail() {
        return memberEmail;
    }

    public long version() {
        return version;
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
