package coin.coinzzickmock.feature.member.application.repository;

import coin.coinzzickmock.feature.member.domain.MemberOAuthIdentity;
import java.util.Optional;

public interface MemberOAuthIdentityRepository {
    Optional<MemberOAuthIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);

    boolean existsByMemberIdAndProvider(Long memberId, String provider);

    MemberOAuthIdentity create(MemberOAuthIdentity identity);
}
