package coin.coinzzickmock.feature.community.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CommunityCommentEntityRepository extends JpaRepository<CommunityCommentEntity, Long> {
    Page<CommunityCommentEntity> findByPostIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Long postId, Pageable pageable);

    Optional<CommunityCommentEntity> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CommunityCommentEntity> findWithLockingById(Long id);
}
