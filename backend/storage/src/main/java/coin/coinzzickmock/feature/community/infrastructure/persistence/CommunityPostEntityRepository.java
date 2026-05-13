package coin.coinzzickmock.feature.community.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CommunityPostEntityRepository extends JpaRepository<CommunityPostEntity, Long> {
    List<CommunityPostEntity> findTop3ByCategoryAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(String category);

    Page<CommunityPostEntity> findByCategoryAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
            String category,
            Pageable pageable
    );

    Page<CommunityPostEntity> findByCategoryNotAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
            String category,
            Pageable pageable
    );

    Optional<CommunityPostEntity> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CommunityPostEntity> findWithLockingById(Long id);
}
