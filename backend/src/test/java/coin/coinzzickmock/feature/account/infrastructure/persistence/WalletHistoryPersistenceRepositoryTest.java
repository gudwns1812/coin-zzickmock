package coin.coinzzickmock.feature.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.repository.WalletHistoryRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
class WalletHistoryPersistenceRepositoryTest {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WalletHistoryRepository walletHistoryRepository;

    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void recordsWalletHistoryOnceForDuplicateSources() {
        MemberCredential member = memberCredentialRepository.save(member("wallet-history-" + System.nanoTime()));
        TradingAccount account = new TradingAccount(
                member.memberId(),
                member.memberEmail(),
                member.memberName(),
                100_000,
                100_000
        );
        String orderId = "duplicate-order-" + member.memberId();
        WalletHistorySource source = WalletHistorySource.orderFill(orderId);

        TradingAccount created = accountRepository.create(account);
        TradingAccount first = accountRepository.updateWithVersion(
                created,
                created.reserveForFilledOrder(10, 100),
                source
        ).updatedAccount();
        accountRepository.updateWithVersion(
                first,
                first.reserveForFilledOrder(10, 100),
                source
        );

        List<WalletHistorySnapshot> snapshots = walletHistoryRepository.findByMemberIdBetween(
                member.memberId(),
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS)
        );

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).sourceType()).isEqualTo(WalletHistorySource.TYPE_ORDER_FILL);
        assertThat(snapshots.get(0).sourceReference()).isEqualTo("order:" + orderId + ":fill");
    }

    @Test
    void createsWalletHistorySourceReferenceAsVarchar255() throws Exception {
        try (var connection = dataSource.getConnection();
             var columns = connection.getMetaData().getColumns(null, null, "WALLET_HISTORY", "SOURCE_REFERENCE")) {
            assertThat(columns.next()).isTrue();
            assertThat(columns.getInt("COLUMN_SIZE")).isEqualTo(255);
        }
    }

    private MemberCredential member(String account) {
        return MemberCredential.register(
                account,
                "hashed-password",
                "Wallet",
                "Wallet",
                account + "@coinzzickmock.dev",
                "010-0000-0000",
                "00000",
                "Seoul",
                "101",
                0
        );
    }
}
