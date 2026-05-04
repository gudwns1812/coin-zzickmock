package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AccountRefillStatePersistenceRepository implements AccountRefillStateRepository {
    private final AccountRefillStateEntityRepository accountRefillStateEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountRefillState> findByMemberIdAndRefillDate(Long memberId, LocalDate refillDate) {
        return accountRefillStateEntityRepository
                .findByMemberIdAndRefillDate(memberId, refillDate)
                .map(AccountRefillStateEntity::toDomain);
    }

    @Override
    @Transactional
    public AccountRefillState ensureByMemberIdAndRefillDateForUpdate(Long memberId, LocalDate refillDate) {
        accountRefillStateEntityRepository.insertDailyStateIfMissing(memberId, refillDate);
        return accountRefillStateEntityRepository
                .findWithLockingByMemberIdAndRefillDate(memberId, refillDate)
                .map(AccountRefillStateEntity::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "리필 상태 행을 생성했지만 조회할 수 없습니다. memberId=%d, refillDate=%s"
                                .formatted(memberId, refillDate)
                ));
    }

    @Override
    @Transactional
    public Optional<AccountRefillState> findByMemberIdAndRefillDateForUpdate(Long memberId, LocalDate refillDate) {
        return accountRefillStateEntityRepository
                .findWithLockingByMemberIdAndRefillDate(memberId, refillDate)
                .map(AccountRefillStateEntity::toDomain);
    }

    @Override
    @Transactional
    public AccountRefillState save(AccountRefillState state) {
        AccountRefillStateEntity entity = accountRefillStateEntityRepository
                .findWithLockingByMemberIdAndRefillDate(state.memberId(), state.refillDate())
                .map(existing -> {
                    existing.apply(state);
                    return existing;
                })
                .orElseGet(() -> AccountRefillStateEntity.from(state));
        return accountRefillStateEntityRepository.save(entity).toDomain();
    }
}
