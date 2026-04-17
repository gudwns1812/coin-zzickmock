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
    public Optional<MemberCredential> findByMemberId(String memberId) {
        return memberCredentialEntityRepository.findById(memberId)
                .map(MemberCredentialEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMemberId(String memberId) {
        return memberCredentialEntityRepository.existsById(memberId);
    }

    @Override
    @Transactional
    public MemberCredential save(MemberCredential memberCredential) {
        MemberCredentialEntity entity = memberCredentialEntityRepository.findById(memberCredential.memberId())
                .map(existing -> {
                    existing.apply(memberCredential);
                    return existing;
                })
                .orElseGet(() -> MemberCredentialEntity.from(memberCredential));
        return memberCredentialEntityRepository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public void deleteByMemberId(String memberId) {
        memberCredentialEntityRepository.deleteById(memberId);
    }
}
