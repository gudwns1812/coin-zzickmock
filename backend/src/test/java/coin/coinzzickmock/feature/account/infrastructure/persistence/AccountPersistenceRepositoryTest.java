package coin.coinzzickmock.feature.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
@Transactional
class AccountPersistenceRepositoryTest {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Test
    void createUsesInsertOnlySemanticsForManuallyAssignedAccountId() {
        MemberCredential member = memberCredentialRepository.save(member("account-insert-only-" + System.nanoTime()));
        TradingAccount account = TradingAccount.openDefault(
                member.memberId(),
                member.memberEmail(),
                member.memberName()
        );

        TradingAccount created = accountRepository.create(account);

        assertThat(created.memberId()).isEqualTo(member.memberId());
        assertThatThrownBy(() -> accountRepository.create(account))
                .isInstanceOf(RuntimeException.class);
    }

    private MemberCredential member(String account) {
        return MemberCredential.register(
                account,
                "hashed-password",
                "Insert Only",
                "Insert Only",
                account + "@coinzzickmock.dev",
                "010-0000-0000",
                "00000",
                "Seoul",
                "101",
                0
        );
    }
}
