package coin.coinzzickmock.feature.member.application.repository;

import coin.coinzzickmock.feature.member.domain.MemberCredential;

import java.util.Optional;

public interface MemberCredentialRepository {
    Optional<MemberCredential> findByMemberId(String memberId);

    boolean existsByMemberId(String memberId);

    MemberCredential save(MemberCredential memberCredential);

    void deleteByMemberId(String memberId);
}
