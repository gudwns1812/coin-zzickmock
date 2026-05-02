package coin.coinzzickmock.feature.member.application.repository;

import coin.coinzzickmock.feature.member.domain.MemberCredential;

import java.util.Optional;

public interface MemberCredentialRepository {
    Optional<MemberCredential> findActiveByMemberId(Long memberId);

    Optional<MemberCredential> findActiveByAccount(String account);

    Optional<MemberCredential> findByAccountIncludingWithdrawn(String account);

    boolean existsByAccount(String account);

    MemberCredential save(MemberCredential memberCredential);
}
