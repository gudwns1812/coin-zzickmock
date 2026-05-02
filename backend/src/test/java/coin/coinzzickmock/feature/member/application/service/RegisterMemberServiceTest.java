package coin.coinzzickmock.feature.member.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

class RegisterMemberServiceTest {
    @Test
    void registerPublishesLeaderboardRefreshEventAfterDefaultAccountCommit() {
        InMemoryMemberCredentialRepository memberCredentialRepository = new InMemoryMemberCredentialRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        RegisterMemberService service = new RegisterMemberService(
                memberCredentialRepository,
                accountRepository,
                new PlainPasswordHasher(),
                new AfterCommitEventPublisher(eventPublisher)
        );

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.register(
                    " new-ranker ",
                    "hello@1234",
                    "New Ranker",
                    "New Ranker",
                    "new-ranker@coinzzickmock.dev",
                    "010-5555-6666",
                    "04524",
                    "서울 중구 세종대로 110",
                    "12층"
            );

            assertEquals(TradingAccount.INITIAL_WALLET_BALANCE, accountRepository.accounts.get(1L).walletBalance());
            assertTrue(eventPublisher.events.isEmpty());

            TransactionSynchronizationUtils.triggerAfterCommit();

            assertEquals(1, eventPublisher.events.size());
            assertEquals(1L, eventPublisher.events.get(1L).memberId());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static class CapturingEventPublisher implements ApplicationEventPublisher {
        private final Map<Long, WalletBalanceChangedEvent> events = new LinkedHashMap<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof WalletBalanceChangedEvent walletBalanceChangedEvent) {
                events.put(walletBalanceChangedEvent.memberId(), walletBalanceChangedEvent);
            }
        }
    }

    private static class InMemoryMemberCredentialRepository implements MemberCredentialRepository {
        private final Map<Long, MemberCredential> credentialsById = new LinkedHashMap<>();
        private final Map<String, MemberCredential> credentialsByAccount = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public Optional<MemberCredential> findActiveByMemberId(Long memberId) {
            return Optional.ofNullable(credentialsById.get(memberId))
                    .filter(memberCredential -> !memberCredential.withdrawn());
        }

        @Override
        public Optional<MemberCredential> findActiveByAccount(String account) {
            return Optional.ofNullable(credentialsByAccount.get(account))
                    .filter(memberCredential -> !memberCredential.withdrawn());
        }

        @Override
        public Optional<MemberCredential> findByAccountIncludingWithdrawn(String account) {
            return Optional.ofNullable(credentialsByAccount.get(account));
        }

        @Override
        public boolean existsByAccount(String account) {
            return credentialsByAccount.containsKey(account);
        }

        @Override
        public MemberCredential save(MemberCredential memberCredential) {
            MemberCredential saved = memberCredential.memberId() == null
                    ? memberCredential.withMemberId(nextId++)
                    : memberCredential;
            credentialsById.put(saved.memberId(), saved);
            credentialsByAccount.put(saved.account(), saved);
            return saved;
        }

    }

    private static class InMemoryAccountRepository implements AccountRepository {
        private final Map<Long, TradingAccount> accounts = new LinkedHashMap<>();

        @Override
        public Optional<TradingAccount> findByMemberId(Long memberId) {
            return Optional.ofNullable(accounts.get(memberId));
        }

        @Override
        public TradingAccount save(TradingAccount account) {
            accounts.put(account.memberId(), account);
            return account;
        }
    }

    private static class PlainPasswordHasher implements MemberPasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return passwordHash.equals(hash(rawPassword));
        }
    }
}
