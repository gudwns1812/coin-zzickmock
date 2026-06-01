package coin.coinzzickmock.feature.member.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberOAuthPendingLinkEntityRepository extends JpaRepository<MemberOAuthPendingLinkEntity, Long> {
    Optional<MemberOAuthPendingLinkEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pendingLink from MemberOAuthPendingLinkEntity pendingLink where pendingLink.tokenHash = :tokenHash")
    Optional<MemberOAuthPendingLinkEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
