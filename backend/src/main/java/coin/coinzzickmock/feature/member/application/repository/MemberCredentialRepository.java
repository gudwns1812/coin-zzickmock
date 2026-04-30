package coin.coinzzickmock.feature.member.application.repository;

import coin.coinzzickmock.feature.member.domain.MemberCredential;

import java.util.Optional;

public interface MemberCredentialRepository {
    Optional<MemberCredential> findByMemberId(Long memberId);

    Optional<MemberCredential> findByAccount(String account);

    boolean existsByAccount(String account);

    MemberCredential save(MemberCredential memberCredential);

    void deleteByMemberId(Long memberId);
}
