package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reward_point_histories")
public class RewardPointHistoryEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, length = 64)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "history_type", nullable = false, length = 50)
    private RewardPointHistoryType historyType;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_reference", length = 100)
    private String sourceReference;

    protected RewardPointHistoryEntity() {
    }

    public static RewardPointHistoryEntity from(RewardPointHistory history) {
        RewardPointHistoryEntity entity = new RewardPointHistoryEntity();
        entity.memberId = history.memberId();
        entity.historyType = history.historyType();
        entity.amount = history.amount();
        entity.balanceAfter = history.balanceAfter();
        entity.sourceType = history.sourceType();
        entity.sourceReference = history.sourceReference();
        return entity;
    }

    public RewardPointHistory toDomain() {
        return new RewardPointHistory(
                memberId,
                historyType,
                amount,
                balanceAfter,
                sourceType,
                sourceReference
        );
    }
}
