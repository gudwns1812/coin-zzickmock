package coin.coinzzickmock.feature.member.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
@Transactional
class MemberCredentialPersistenceRepositoryTest {
    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void findActiveByAccountExcludesWithdrawn() {
        MemberCredential withdrawn = memberCredentialRepository.create(member("withdrawn-active-account"));
        memberCredentialRepository.save(withdrawn.withdraw(Instant.parse("2026-05-02T00:00:00Z")));

        assertThat(memberCredentialRepository.findActiveByAccount("withdrawn-active-account")).isEmpty();
        assertThat(memberCredentialRepository.findByAccountIncludingWithdrawn("withdrawn-active-account"))
                .get()
                .extracting(MemberCredential::withdrawn)
                .isEqualTo(true);
    }

    @Test
    void findActiveByMemberIdExcludesWithdrawn() {
        MemberCredential withdrawn = memberCredentialRepository.create(member("withdrawn-active-id"));
        memberCredentialRepository.save(withdrawn.withdraw(Instant.parse("2026-05-02T00:00:00Z")));

        assertThat(memberCredentialRepository.findActiveByMemberId(withdrawn.memberId())).isEmpty();
    }

    @Test
    void existsByAccountIncludesWithdrawn() {
        MemberCredential withdrawn = memberCredentialRepository.create(member("withdrawn-duplicate"));
        memberCredentialRepository.save(withdrawn.withdraw(Instant.parse("2026-05-02T00:00:00Z")));

        assertThat(memberCredentialRepository.existsByAccount("withdrawn-duplicate")).isTrue();
    }

    @Test
    void createTranslatesDuplicateAccountUniqueConstraint() {
        memberCredentialRepository.create(member("duplicate-account"));

        assertThatThrownBy(() -> memberCredentialRepository.create(member("duplicate-account")))
                .isInstanceOf(CoreException.class)
                .extracting(exception -> ((CoreException) exception).errorCode())
                .isEqualTo(ErrorCode.MEMBER_ALREADY_EXISTS);
    }

    @Test
    void createTreatsWithdrawnAccountAsDuplicate() {
        MemberCredential withdrawn = memberCredentialRepository.create(member("withdrawn-create-dupe"));
        memberCredentialRepository.save(withdrawn.withdraw(Instant.parse("2026-05-02T00:00:00Z")));

        assertThatThrownBy(() -> memberCredentialRepository.create(member("withdrawn-create-dupe")))
                .isInstanceOf(CoreException.class)
                .extracting(exception -> ((CoreException) exception).errorCode())
                .isEqualTo(ErrorCode.MEMBER_ALREADY_EXISTS);
    }

    @Test
    void existingRowsAreActiveAndWithdrawnAtIsNullable() throws Exception {
        assertThat(memberCredentialRepository.findActiveByAccount("test")).isPresent();

        try (var connection = dataSource.getConnection();
             var columns = connection.getMetaData().getColumns(null, null, "MEMBER_CREDENTIALS", "WITHDRAWN_AT")) {
            assertThat(columns.next()).isTrue();
            assertThat(columns.getInt("NULLABLE")).isEqualTo(java.sql.DatabaseMetaData.columnNullable);
        }
    }

    private MemberCredential member(String account) {
        return MemberCredential.register(
                account,
                "hashed-password",
                "Member " + account,
                "Nick " + account,
                account + "@coinzzickmock.dev",
                "010-0000-0000",
                "00000",
                "Seoul",
                "101",
                0
        );
    }
}
