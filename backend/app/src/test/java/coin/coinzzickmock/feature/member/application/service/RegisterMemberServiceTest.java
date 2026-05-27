package coin.coinzzickmock.feature.member.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.dto.AccountMutationResult;
import coin.coinzzickmock.feature.account.application.event.TradingAccountOpenedEvent;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.service.AccountRefillDatePolicy;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.member.application.dto.MemberProfileResult;
import coin.coinzzickmock.feature.member.application.implement.MemberRegistrationProvisioner;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class RegisterMemberServiceTest {
    @Test
    void registerCreatesCredentialDefaultTradingAccountAndRefillState() {
        InMemoryMemberCredentialRepository memberCredentialRepository = new InMemoryMemberCredentialRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryAccountRefillStateRepository refillStateRepository = new InMemoryAccountRefillStateRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        RegisterMemberService service = registerMemberService(
                memberCredentialRepository,
                accountRepository,
                refillStateRepository,
                eventPublisher
        );

        MemberProfileResult result = service.register(
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

        TradingAccount account = accountRepository.accountsByMemberId.get(result.memberId());
        assertEquals(TradingAccount.INITIAL_WALLET_BALANCE, account.walletBalance());
        assertEquals(TradingAccount.INITIAL_AVAILABLE_MARGIN, account.availableMargin());
        assertEquals(1, refillStateRepository.statesByKey.size());

        assertEquals(1, eventPublisher.openedEvents.size());
        TradingAccountOpenedEvent event = eventPublisher.openedEvents.get(result.memberId());
        assertEquals(result.memberId(), event.memberId());
        assertEquals("new-ranker@coinzzickmock.dev", event.memberEmail());
        assertEquals("New Ranker", event.memberName());
        assertEquals("New Ranker", event.nickname());
    }

    @Test
    void registerReliesOnRepositoryCreateToRejectDuplicateAccount() {
        InMemoryMemberCredentialRepository memberCredentialRepository = new InMemoryMemberCredentialRepository();
        RegisterMemberService service = registerMemberService(
                memberCredentialRepository,
                new InMemoryAccountRepository(),
                new InMemoryAccountRefillStateRepository(),
                new CapturingEventPublisher()
        );

        service.register(
                "duplicate-ranker",
                "hello@1234",
                "Duplicate Ranker",
                "Duplicate Ranker",
                "duplicate-ranker@coinzzickmock.dev",
                "010-5555-6666",
                "04524",
                "서울 중구 세종대로 110",
                "12층"
        );

        CoreException exception = assertThrows(CoreException.class, () -> service.register(
                " duplicate-ranker ",
                "hello@1234",
                "Duplicate Ranker 2",
                "Duplicate Ranker 2",
                "duplicate-ranker-2@coinzzickmock.dev",
                "010-7777-8888",
                "04524",
                "서울 중구 세종대로 110",
                "13층"
        ));
        assertEquals(ErrorCode.MEMBER_ALREADY_EXISTS, exception.errorCode());
    }

    private static RegisterMemberService registerMemberService(
            InMemoryMemberCredentialRepository memberCredentialRepository,
            InMemoryAccountRepository accountRepository,
            InMemoryAccountRefillStateRepository refillStateRepository,
            CapturingEventPublisher eventPublisher
    ) {
        return new RegisterMemberService(
                new PlainPasswordHasher(),
                new MemberRegistrationProvisioner(
                        memberCredentialRepository,
                        accountRepository,
                        refillStateRepository,
                        new AccountRefillDatePolicy(),
                        new AfterCommitEventPublisher(eventPublisher)
                )
        );
    }

    private static class CapturingEventPublisher implements ApplicationEventPublisher {
        private final Map<Long, TradingAccountOpenedEvent> openedEvents = new LinkedHashMap<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof TradingAccountOpenedEvent tradingAccountOpenedEvent) {
                openedEvents.put(tradingAccountOpenedEvent.memberId(), tradingAccountOpenedEvent);
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
        public MemberCredential create(MemberCredential memberCredential) {
            if (credentialsByAccount.containsKey(memberCredential.account())) {
                throw new CoreException(ErrorCode.MEMBER_ALREADY_EXISTS);
            }
            return save(memberCredential);
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
        private final Map<Long, TradingAccount> accountsByMemberId = new LinkedHashMap<>();

        @Override
        public Optional<TradingAccount> findByMemberId(Long memberId) {
            return Optional.ofNullable(accountsByMemberId.get(memberId));
        }

        @Override
        public Optional<TradingAccount> findByMemberIdForUpdate(Long memberId) {
            return findByMemberId(memberId);
        }

        @Override
        public List<TradingAccount> findAll() {
            return new ArrayList<>(accountsByMemberId.values());
        }

        @Override
        public TradingAccount create(TradingAccount account) {
            accountsByMemberId.put(account.memberId(), account);
            return account;
        }

        @Override
        public AccountMutationResult updateWithVersion(TradingAccount expectedAccount, TradingAccount nextAccount) {
            throw new UnsupportedOperationException("not used by register tests");
        }
    }

    private static class InMemoryAccountRefillStateRepository implements AccountRefillStateRepository {
        private final Map<String, AccountRefillState> statesByKey = new LinkedHashMap<>();

        @Override
        public Optional<AccountRefillState> findByMemberIdAndRefillDate(Long memberId, LocalDate refillDate) {
            return Optional.ofNullable(statesByKey.get(key(memberId, refillDate)));
        }

        @Override
        public void provisionWeeklyStateIfAbsent(Long memberId, LocalDate refillDate) {
            statesByKey.putIfAbsent(key(memberId, refillDate), AccountRefillState.weekly(memberId, refillDate));
        }

        @Override
        public AccountRefillState grantExtraRefillCount(Long memberId, LocalDate refillDate, int count) {
            throw new UnsupportedOperationException("not used by register tests");
        }

        @Override
        public Optional<LockedAccountRefillState> findByMemberIdAndRefillDateForUpdate(
                Long memberId,
                LocalDate refillDate
        ) {
            throw new UnsupportedOperationException("not used by register tests");
        }

        private String key(Long memberId, LocalDate refillDate) {
            return memberId + ":" + refillDate;
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
