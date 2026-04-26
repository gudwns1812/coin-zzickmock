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
                    "new-ranker@coinzzickmock.dev",
                    "010-5555-6666",
                    "04524",
                    "서울 중구 세종대로 110",
                    "12층"
            );

            assertEquals(TradingAccount.INITIAL_WALLET_BALANCE, accountRepository.accounts.get("new-ranker").walletBalance());
            assertTrue(eventPublisher.events.isEmpty());

            TransactionSynchronizationUtils.triggerAfterCommit();

            assertEquals(1, eventPublisher.events.size());
            assertEquals("new-ranker", eventPublisher.events.get("new-ranker").memberId());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static class CapturingEventPublisher implements ApplicationEventPublisher {
        private final Map<String, WalletBalanceChangedEvent> events = new LinkedHashMap<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof WalletBalanceChangedEvent walletBalanceChangedEvent) {
                events.put(walletBalanceChangedEvent.memberId(), walletBalanceChangedEvent);
            }
        }
    }

    private static class InMemoryMemberCredentialRepository implements MemberCredentialRepository {
        private final Map<String, MemberCredential> credentials = new LinkedHashMap<>();

        @Override
        public Optional<MemberCredential> findByMemberId(String memberId) {
            return Optional.ofNullable(credentials.get(memberId));
        }

        @Override
        public boolean existsByMemberId(String memberId) {
            return credentials.containsKey(memberId);
        }

        @Override
        public MemberCredential save(MemberCredential memberCredential) {
            credentials.put(memberCredential.memberId(), memberCredential);
            return memberCredential;
        }

        @Override
        public void deleteByMemberId(String memberId) {
            credentials.remove(memberId);
        }
    }

    private static class InMemoryAccountRepository implements AccountRepository {
        private final Map<String, TradingAccount> accounts = new LinkedHashMap<>();

        @Override
        public Optional<TradingAccount> findByMemberId(String memberId) {
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
