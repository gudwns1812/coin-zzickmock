package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class JpaMemberCredentialRepository implements MemberCredentialRepository {
    private final MemberCredentialSpringDataRepository memberCredentialSpringDataRepository;

    public JpaMemberCredentialRepository(MemberCredentialSpringDataRepository memberCredentialSpringDataRepository) {
        this.memberCredentialSpringDataRepository = memberCredentialSpringDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberCredential> findByMemberId(String memberId) {
        return memberCredentialSpringDataRepository.findById(memberId)
                .map(MemberCredentialEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMemberId(String memberId) {
        return memberCredentialSpringDataRepository.existsById(memberId);
    }

    @Override
    @Transactional
    public MemberCredential save(MemberCredential memberCredential) {
        MemberCredentialEntity entity = memberCredentialSpringDataRepository.findById(memberCredential.memberId())
                .map(existing -> {
                    existing.apply(memberCredential);
                    return existing;
                })
                .orElseGet(() -> MemberCredentialEntity.from(memberCredential));
        return memberCredentialSpringDataRepository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public void deleteByMemberId(String memberId) {
        memberCredentialSpringDataRepository.deleteById(memberId);
    }
}
