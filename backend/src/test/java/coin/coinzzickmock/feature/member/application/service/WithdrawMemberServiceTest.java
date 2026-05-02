package coin.coinzzickmock.feature.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class WithdrawMemberServiceTest {
    private static final Instant WITHDRAWN_AT = Instant.parse("2026-05-02T10:15:30Z");

    @Test
    void withdrawMarksMemberCredentialWithdrawn() {
        InMemoryMemberCredentialRepository repository = new InMemoryMemberCredentialRepository();
        MemberCredential member = repository.save(member("soft-delete-user"));
        WithdrawMemberService service = new WithdrawMemberService(
                repository,
                Clock.fixed(WITHDRAWN_AT, ZoneOffset.UTC),
                new AfterCommitEventPublisher(new CapturingEventPublisher())
        );

        service.withdraw(member.memberId(), member.memberId());

        assertThat(repository.findActiveByMemberId(member.memberId())).isEmpty();
        assertThat(repository.findByAccountIncludingWithdrawn("soft-delete-user"))
                .get()
                .extracting(MemberCredential::withdrawnAt)
                .isEqualTo(WITHDRAWN_AT);
    }

    @Test
    void withdrawRejectsAnotherMemberId() {
        WithdrawMemberService service = new WithdrawMemberService(
                new InMemoryMemberCredentialRepository(),
                Clock.fixed(WITHDRAWN_AT, ZoneOffset.UTC),
                new AfterCommitEventPublisher(new CapturingEventPublisher())
        );

        assertThrows(CoreException.class, () -> service.withdraw(1L, 2L));
    }

    @Test
    void withdrawRejectsAlreadyWithdrawnMember() {
        InMemoryMemberCredentialRepository repository = new InMemoryMemberCredentialRepository();
        MemberCredential member = repository.save(member("already-withdrawn"));
        repository.save(member.withdraw(WITHDRAWN_AT));
        WithdrawMemberService service = new WithdrawMemberService(
                repository,
                Clock.fixed(WITHDRAWN_AT, ZoneOffset.UTC),
                new AfterCommitEventPublisher(new CapturingEventPublisher())
        );

        assertThrows(CoreException.class, () -> service.withdraw(member.memberId(), member.memberId()));
    }

    @Test
    void withdrawPublishesLeaderboardRefreshEvent() {
        InMemoryMemberCredentialRepository repository = new InMemoryMemberCredentialRepository();
        MemberCredential member = repository.save(member("leaderboard-withdrawn"));
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        WithdrawMemberService service = new WithdrawMemberService(
                repository,
                Clock.fixed(WITHDRAWN_AT, ZoneOffset.UTC),
                new AfterCommitEventPublisher(eventPublisher)
        );

        service.withdraw(member.memberId(), member.memberId());

        assertThat(eventPublisher.walletBalanceChangedEvents)
                .extracting(WalletBalanceChangedEvent::memberId)
                .containsExactly(member.memberId());
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

    private static class CapturingEventPublisher implements ApplicationEventPublisher {
        private final java.util.ArrayList<WalletBalanceChangedEvent> walletBalanceChangedEvents =
                new java.util.ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof WalletBalanceChangedEvent walletBalanceChangedEvent) {
                walletBalanceChangedEvents.add(walletBalanceChangedEvent);
            }
        }
    }
}
