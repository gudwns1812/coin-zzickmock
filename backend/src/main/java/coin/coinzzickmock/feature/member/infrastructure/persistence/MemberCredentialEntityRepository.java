package coin.coinzzickmock.feature.member.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCredentialEntityRepository extends JpaRepository<MemberCredentialEntity, String> {
    void deleteAllByMemberId(String memberId);
}
