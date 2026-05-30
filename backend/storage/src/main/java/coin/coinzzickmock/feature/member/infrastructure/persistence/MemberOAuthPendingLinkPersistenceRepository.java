package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.feature.member.application.repository.MemberOAuthPendingLinkRepository;
import coin.coinzzickmock.feature.member.domain.MemberOAuthPendingLink;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class MemberOAuthPendingLinkPersistenceRepository implements MemberOAuthPendingLinkRepository {
    private final MemberOAuthPendingLinkEntityRepository memberOAuthPendingLinkEntityRepository;

    @Override
    @Transactional
    public MemberOAuthPendingLink create(MemberOAuthPendingLink pendingLink) {
        return memberOAuthPendingLinkEntityRepository.saveAndFlush(MemberOAuthPendingLinkEntity.from(pendingLink))
                .toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberOAuthPendingLink> findByTokenHash(String tokenHash) {
        return memberOAuthPendingLinkEntityRepository.findByTokenHash(tokenHash)
                .map(MemberOAuthPendingLinkEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<MemberOAuthPendingLink> findByTokenHashForUpdate(String tokenHash) {
        return memberOAuthPendingLinkEntityRepository.findByTokenHashForUpdate(tokenHash)
                .map(MemberOAuthPendingLinkEntity::toDomain);
    }

    @Override
    @Transactional
    public MemberOAuthPendingLink save(MemberOAuthPendingLink pendingLink) {
        MemberOAuthPendingLinkEntity entity = memberOAuthPendingLinkEntityRepository.findById(pendingLink.id())
                .orElseGet(() -> MemberOAuthPendingLinkEntity.from(pendingLink));
        entity.apply(pendingLink);
        return memberOAuthPendingLinkEntityRepository.save(entity).toDomain();
    }
}
