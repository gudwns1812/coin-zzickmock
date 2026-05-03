package coin.coinzzickmock.feature.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
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

        TradingAccount persisted = accountRepository.findByMemberId(member.memberId()).orElseThrow();
        assertThat(persisted.walletBalance()).isEqualTo(TradingAccount.INITIAL_WALLET_BALANCE);
        assertThat(persisted.availableMargin()).isEqualTo(TradingAccount.INITIAL_WALLET_BALANCE);
        assertThat(persisted.version()).isZero();
    }

    @Test
    void updateWithVersionMutatesOnlyWhenExpectedVersionMatches() {
        MemberCredential member = memberCredentialRepository.save(member("account-version-guard-" + System.nanoTime()));
        TradingAccount created = accountRepository.create(TradingAccount.openDefault(
                member.memberId(),
                member.memberEmail(),
                member.memberName()
        ));

        AccountMutationResult first = accountRepository.updateWithVersion(
                created,
                created.reserveForFilledOrder(10, 100),
                WalletHistorySource.orderFill("guarded-account-order")
        );

        assertThat(first.status()).isEqualTo(AccountMutationResult.Status.UPDATED);
        assertThat(first.updatedAccount().version()).isEqualTo(1);
        assertThat(first.updatedAccount().walletBalance()).isEqualTo(99_990d);
        assertThat(first.updatedAccount().availableMargin()).isEqualTo(99_890d);

        AccountMutationResult stale = accountRepository.updateWithVersion(
                created,
                created.reserveForFilledOrder(20, 200),
                WalletHistorySource.orderFill("stale-account-order")
        );

        assertThat(stale.status()).isEqualTo(AccountMutationResult.Status.STALE_VERSION);
        TradingAccount persisted = accountRepository.findByMemberId(member.memberId()).orElseThrow();
        assertThat(persisted.version()).isEqualTo(1);
        assertThat(persisted.walletBalance()).isEqualTo(99_990d);
        assertThat(persisted.availableMargin()).isEqualTo(99_890d);
    }

    @Test
    void updateWithVersionReportsNotFoundWithoutUpserting() {
        TradingAccount missing = TradingAccount.openDefault(
                -System.nanoTime(),
                "missing-account@coinzzickmock.dev",
                "Missing Account"
        );

        AccountMutationResult result = accountRepository.updateWithVersion(
                missing,
                missing.reserveForFilledOrder(10, 100),
                WalletHistorySource.orderFill("missing-account-order")
        );

        assertThat(result.status()).isEqualTo(AccountMutationResult.Status.NOT_FOUND);
        assertThat(accountRepository.findByMemberId(missing.memberId())).isEmpty();
    }

    @Test
    void updateWithVersionRequiresWalletHistorySource() {
        MemberCredential member = memberCredentialRepository.save(member("account-source-required-" + System.nanoTime()));
        TradingAccount created = accountRepository.create(TradingAccount.openDefault(
                member.memberId(),
                member.memberEmail(),
                member.memberName()
        ));

        assertThatThrownBy(() -> accountRepository.updateWithVersion(
                created,
                created.reserveForFilledOrder(10, 100),
                null
        )).hasMessageContaining("WalletHistorySource");
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
