package coin.coinzzickmock.feature.member.infrastructure.auth;

import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberRole;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.ActorLookup;
import coin.coinzzickmock.providers.auth.ActorRole;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberActorLookup implements ActorLookup {
    private final MemberCredentialRepository memberCredentialRepository;

    @Override
    public Optional<Actor> findByMemberId(Long memberId) {
        return memberCredentialRepository.findByMemberId(memberId)
                .map(this::toActor);
    }

    @Override
    public Optional<Actor> findByAccount(String account) {
        return memberCredentialRepository.findByAccount(account)
                .map(this::toActor);
    }

    private Actor toActor(MemberCredential memberCredential) {
        return new Actor(
                memberCredential.memberId(),
                memberCredential.account(),
                memberCredential.memberEmail(),
                memberCredential.nickname(),
                toActorRole(memberCredential.role())
        );
    }

    private ActorRole toActorRole(MemberRole role) {
        return role == MemberRole.ADMIN ? ActorRole.ADMIN : ActorRole.USER;
    }
}
