package coin.coinzzickmock.feature.member.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberOAuthIdentityEntityRepository extends JpaRepository<MemberOAuthIdentityEntity, Long> {
    Optional<MemberOAuthIdentityEntity> findByProviderAndProviderSubject(String provider, String providerSubject);

    boolean existsByMemberIdAndProvider(Long memberId, String provider);
}
