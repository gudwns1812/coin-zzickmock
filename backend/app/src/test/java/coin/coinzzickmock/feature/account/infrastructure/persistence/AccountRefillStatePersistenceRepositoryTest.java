package coin.coinzzickmock.feature.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
@Transactional
class AccountRefillStatePersistenceRepositoryTest {
    private static final LocalDate REFILL_DATE = LocalDate.of(2026, 5, 4);

    @Autowired
    private AccountRefillStateRepository accountRefillStateRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void provisionDailyStateIfAbsentDoesNotResetExistingCount() {
        Long memberId = saveMember();

        accountRefillStateRepository.provisionDailyStateIfAbsent(memberId, REFILL_DATE);
        accountRefillStateRepository.grantExtraRefillCount(memberId, REFILL_DATE, 2);
        accountRefillStateRepository.provisionDailyStateIfAbsent(memberId, REFILL_DATE);

        AccountRefillState state = accountRefillStateRepository
                .findByMemberIdAndRefillDate(memberId, REFILL_DATE)
                .orElseThrow();
        assertEquals(3, state.remainingCount());
    }

    @Test
    void grantExtraRefillCountCreatesDailyBaseAndAddsExtraCount() {
        Long memberId = saveMember();

        AccountRefillState state = accountRefillStateRepository.grantExtraRefillCount(memberId, REFILL_DATE, 1);

        assertEquals(2, state.remainingCount());
        assertEquals(0, state.version());
    }

    @Test
    void grantExtraRefillCountAdvancesVersionWhenUpdatingExistingState() {
        Long memberId = saveMember();
        accountRefillStateRepository.provisionDailyStateIfAbsent(memberId, REFILL_DATE);

        AccountRefillState state = accountRefillStateRepository.grantExtraRefillCount(memberId, REFILL_DATE, 1);

        assertEquals(2, state.remainingCount());
        assertEquals(1, state.version());
    }

    @Test
    void lockedStateConsumesCountWithDirtyChecking() {
        Long memberId = saveMember();
        accountRefillStateRepository.grantExtraRefillCount(memberId, REFILL_DATE, 1);

        AccountRefillState consumed = accountRefillStateRepository
                .findByMemberIdAndRefillDateForUpdate(memberId, REFILL_DATE)
                .orElseThrow()
                .consumeOne();

        assertEquals(1, consumed.remainingCount());
        assertThat(accountRefillStateRepository.findByMemberIdAndRefillDate(memberId, REFILL_DATE))
                .get()
                .extracting(AccountRefillState::remainingCount)
                .isEqualTo(1);
    }

    private Long saveMember() {
        long memberId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        jdbcTemplate.update("""
                INSERT INTO member_credentials (
                    id, account, password_hash, member_name, nickname, member_email,
                    phone_number, zip_code, address, address_detail, invest_score, role
                )
                VALUES (?, ?, 'hash', ?, ?, ?, '010-0000-0000', '00000', '서울', '', 0, 'USER')
                """,
                memberId,
                "refill-account-" + memberId,
                "Member " + memberId,
                "Nick " + memberId,
                memberId + "@coinzzickmock.dev"
        );
        return memberId;
    }
}
