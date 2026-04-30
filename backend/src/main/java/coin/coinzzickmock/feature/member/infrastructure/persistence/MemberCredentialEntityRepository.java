package coin.coinzzickmock.feature.member.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberCredentialEntityRepository extends JpaRepository<MemberCredentialEntity, Long> {
    Optional<MemberCredentialEntity> findByAccount(String account);

    boolean existsByAccount(String account);
}
