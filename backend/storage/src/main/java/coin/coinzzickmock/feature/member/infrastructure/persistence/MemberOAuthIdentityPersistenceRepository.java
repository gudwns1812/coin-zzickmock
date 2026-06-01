package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberOAuthIdentityRepository;
import coin.coinzzickmock.feature.member.domain.MemberOAuthIdentity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class MemberOAuthIdentityPersistenceRepository implements MemberOAuthIdentityRepository {
    private final MemberOAuthIdentityEntityRepository memberOAuthIdentityEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberOAuthIdentity> findByProviderAndProviderSubject(String provider, String providerSubject) {
        return memberOAuthIdentityEntityRepository.findByProviderAndProviderSubject(provider, providerSubject)
                .map(MemberOAuthIdentityEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMemberIdAndProvider(Long memberId, String provider) {
        return memberOAuthIdentityEntityRepository.existsByMemberIdAndProvider(memberId, provider);
    }

    @Override
    @Transactional
    public MemberOAuthIdentity create(MemberOAuthIdentity identity) {
        try {
            return memberOAuthIdentityEntityRepository.saveAndFlush(MemberOAuthIdentityEntity.from(identity))
                    .toDomain();
        } catch (DataIntegrityViolationException exception) {
            throw new CoreException(ErrorCode.OAUTH_IDENTITY_ALREADY_LINKED);
        }
    }
}
