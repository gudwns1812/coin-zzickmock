package coin.coinzzickmock.feature.member.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.member.application.event.MemberRegisteredEvent;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class RegisterMemberServiceTest {
    @Test
    void registerPublishesMemberRegisteredEventAfterCredentialSave() {
        InMemoryMemberCredentialRepository memberCredentialRepository = new InMemoryMemberCredentialRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        RegisterMemberService service = new RegisterMemberService(
                memberCredentialRepository,
                new PlainPasswordHasher(),
                eventPublisher
        );

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

        assertEquals(1, eventPublisher.events.size());
        MemberRegisteredEvent event = eventPublisher.events.get(1L);
        assertEquals(1L, event.memberId());
        assertEquals("new-ranker", event.account());
        assertEquals("New Ranker", event.memberName());
        assertEquals("new-ranker@coinzzickmock.dev", event.memberEmail());
    }

    private static class CapturingEventPublisher implements ApplicationEventPublisher {
        private final Map<Long, MemberRegisteredEvent> events = new LinkedHashMap<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof MemberRegisteredEvent memberRegisteredEvent) {
                events.put(memberRegisteredEvent.memberId(), memberRegisteredEvent);
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
