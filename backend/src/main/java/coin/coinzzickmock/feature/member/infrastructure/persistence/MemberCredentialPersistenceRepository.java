package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberCredentialPersistenceRepository implements MemberCredentialRepository {
    private final MemberCredentialEntityRepository memberCredentialEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberCredential> findByMemberId(Long memberId) {
        return memberCredentialEntityRepository.findById(memberId)
                .map(MemberCredentialEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberCredential> findByAccount(String account) {
        return memberCredentialEntityRepository.findByAccount(account)
                .map(MemberCredentialEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAccount(String account) {
        return memberCredentialEntityRepository.existsByAccount(account);
    }

    @Override
    @Transactional
    public MemberCredential save(MemberCredential memberCredential) {
        Optional<MemberCredentialEntity> existingEntity = memberCredential.memberId() == null
                ? memberCredentialEntityRepository.findByAccount(memberCredential.account())
                : memberCredentialEntityRepository.findById(memberCredential.memberId());

        MemberCredentialEntity entity = existingEntity
                .map(existing -> {
                    existing.apply(memberCredential);
                    return existing;
                })
                .orElseGet(() -> MemberCredentialEntity.from(memberCredential));
        return memberCredentialEntityRepository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public void deleteByMemberId(Long memberId) {
        memberCredentialEntityRepository.deleteById(memberId);
    }
}
