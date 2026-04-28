package coin.coinzzickmock.feature.member.infrastructure.auth;

import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.ActorLookup;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberActorLookup implements ActorLookup {
    private final MemberCredentialRepository memberCredentialRepository;

    @Override
    public Optional<Actor> findByMemberId(String memberId) {
        return memberCredentialRepository.findByMemberId(memberId)
                .map(memberCredential -> new Actor(
                        memberCredential.memberId(),
                        memberCredential.memberEmail(),
                        memberCredential.memberName(),
                        memberCredential.role()
                ));
    }
}
