package coin.coinzzickmock.feature.community.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostImageEntityRepository extends JpaRepository<CommunityPostImageEntity, Long> {
    Optional<CommunityPostImageEntity> findByObjectKey(String objectKey);

    List<CommunityPostImageEntity> findByObjectKeyIn(Collection<String> objectKeys);

    List<CommunityPostImageEntity> findByUploaderMemberIdAndStatusAndCreatedAtBefore(
            Long uploaderMemberId,
            String status,
            Instant createdAt
    );

    List<CommunityPostImageEntity> findByPostIdAndStatus(Long postId, String status);
}
