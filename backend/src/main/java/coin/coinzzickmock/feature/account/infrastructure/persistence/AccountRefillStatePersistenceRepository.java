package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository.LockedAccountRefillState;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AccountRefillStatePersistenceRepository implements AccountRefillStateRepository {
    private final AccountRefillStateEntityRepository accountRefillStateEntityRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountRefillState> findByMemberIdAndRefillDate(Long memberId, LocalDate refillDate) {
        return accountRefillStateEntityRepository
                .findByMemberIdAndRefillDate(memberId, refillDate)
                .map(AccountRefillStateEntity::toDomain);
    }

    @Override
    @Transactional
    public void provisionDailyStateIfAbsent(Long memberId, LocalDate refillDate) {
        jdbcTemplate.update(
                """
                        INSERT IGNORE INTO account_refill_states (
                            member_id, refill_date, remaining_count, version
                        )
                        VALUES (?, ?, 1, 0)
                        """,
                memberId,
                refillDate
        );
    }

    @Override
    @Transactional
    public AccountRefillState grantExtraRefillCount(Long memberId, LocalDate refillDate, int count) {
        if (count <= 0) {
            throw invalid();
        }
        jdbcTemplate.update(
                """
                        INSERT INTO account_refill_states (
                            member_id, refill_date, remaining_count, version
                        )
                        VALUES (?, ?, 1 + ?, 0)
                        ON DUPLICATE KEY UPDATE
                            remaining_count = remaining_count + ?,
                            version = version + 1,
                            updated_at = CURRENT_TIMESTAMP(6)
                        """,
                memberId,
                refillDate,
                count,
                count
        );
        return accountRefillStateEntityRepository.findByMemberIdAndRefillDate(memberId, refillDate)
                .map(AccountRefillStateEntity::toDomain)
                .orElseThrow(() -> {
                    log.error("Account refill state was not found after provisioning. operation=provision_daily_refill_state");
                    return new CoreException(ErrorCode.INTERNAL_SERVER_ERROR);
                });
    }

    @Override
    @Transactional
    public Optional<LockedAccountRefillState> findByMemberIdAndRefillDateForUpdate(Long memberId, LocalDate refillDate) {
        return accountRefillStateEntityRepository
                .findWithLockingByMemberIdAndRefillDate(memberId, refillDate)
                .map(ManagedLockedAccountRefillState::new);
    }

    private CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }

    private record ManagedLockedAccountRefillState(AccountRefillStateEntity entity) implements LockedAccountRefillState {
        @Override
        public AccountRefillState state() {
            return entity.toDomain();
        }

        @Override
        public AccountRefillState consumeOne() {
            return entity.consumeOne();
        }
    }
}
