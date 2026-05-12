package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account_refill_states")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountRefillStateEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "refill_date", nullable = false)
    private LocalDate refillDate;

    @Column(name = "remaining_count", nullable = false)
    private int remainingCount;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static AccountRefillStateEntity from(AccountRefillState state) {
        AccountRefillStateEntity entity = new AccountRefillStateEntity();
        entity.apply(state);
        return entity;
    }

    public void apply(AccountRefillState state) {
        this.memberId = state.memberId();
        this.refillDate = state.refillDate();
        this.remainingCount = state.remainingCount();
    }

    public AccountRefillState consumeOne() {
        AccountRefillState consumed = toDomain().consumeOne();
        apply(consumed);
        return consumed;
    }

    public AccountRefillState toDomain() {
        return new AccountRefillState(
                memberId,
                refillDate,
                remainingCount,
                version == null ? 0 : version
        );
    }
}
