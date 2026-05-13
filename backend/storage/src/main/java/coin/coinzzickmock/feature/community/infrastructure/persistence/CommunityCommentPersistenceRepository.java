package coin.coinzzickmock.feature.community.infrastructure.persistence;

import coin.coinzzickmock.feature.community.application.repository.CommunityCommentPage;
import coin.coinzzickmock.feature.community.application.repository.CommunityCommentRepository;
import coin.coinzzickmock.feature.community.domain.CommunityComment;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class CommunityCommentPersistenceRepository implements CommunityCommentRepository {
    private final CommunityCommentEntityRepository commentEntityRepository;
    private final CommunityPostRepositorySupport postRepositorySupport;

    @Override
    @Transactional(readOnly = true)
    public CommunityCommentPage findActiveByPostId(Long postId, int page, int size) {
        var result = commentEntityRepository.findByPostIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(postId, PageRequest.of(page, size));
        return new CommunityCommentPage(result.getContent().stream().map(CommunityCommentEntity::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommunityComment> findActiveById(Long commentId) {
        return commentEntityRepository.findByIdAndDeletedAtIsNull(commentId).map(CommunityCommentEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<CommunityComment> findActiveByIdForUpdate(Long commentId) {
        return commentEntityRepository.findWithLockingById(commentId).filter(comment -> comment.deletedAt() == null).map(CommunityCommentEntity::toDomain);
    }

    @Override
    @Transactional
    public CommunityComment save(CommunityComment comment) {
        CommunityCommentEntity entity = comment.id() == null ? CommunityCommentEntity.from(comment)
                : commentEntityRepository.findById(comment.id()).map(existing -> { existing.apply(comment); return existing; })
                .orElseGet(() -> CommunityCommentEntity.from(comment));
        return commentEntityRepository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public void softDelete(Long commentId, Instant deletedAt) {
        commentEntityRepository.findWithLockingById(commentId).ifPresent(comment -> {
            comment.softDelete(deletedAt);
            postRepositorySupport.decrementCommentCount(comment.postId());
        });
    }
}
