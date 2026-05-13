package coin.coinzzickmock.feature.community.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class CommunityPostPersistenceRepository {
    private static final String NOTICE_CATEGORY = "NOTICE";
    private static final String ATTACHED_STATUS = "ATTACHED";
    private static final Set<String> ATTACHABLE_IMAGE_STATUSES = Set.of("PRESIGNED", ATTACHED_STATUS);

    private final CommunityPostEntityRepository postEntityRepository;
    private final CommunityCommentEntityRepository commentEntityRepository;
    private final CommunityPostLikeEntityRepository likeEntityRepository;
    private final CommunityPostImageEntityRepository imageEntityRepository;

    @Transactional(readOnly = true)
    public List<CommunityPostEntity> findLatestNoticePosts() {
        return postEntityRepository.findTop3ByCategoryAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(NOTICE_CATEGORY);
    }

    @Transactional(readOnly = true)
    public Page<CommunityPostEntity> findNormalPosts(String category, Pageable pageable) {
        if (category == null || category.isBlank() || NOTICE_CATEGORY.equals(category)) {
            return postEntityRepository.findByCategoryNotAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                    NOTICE_CATEGORY,
                    pageable
            );
        }
        return postEntityRepository.findByCategoryAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(category, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<CommunityPostEntity> findActivePost(Long postId) {
        return postEntityRepository.findByIdAndDeletedAtIsNull(postId);
    }

    @Transactional
    public CommunityPostEntity savePost(CommunityPostEntity post) {
        return postEntityRepository.save(post);
    }

    @Transactional
    public Optional<CommunityPostEntity> softDeletePost(Long postId, Instant deletedAt) {
        return postEntityRepository.findWithLockingById(postId)
                .filter(post -> post.deletedAt() == null)
                .map(post -> {
                    post.softDelete(deletedAt);
                    return post;
                });
    }

    @Transactional
    public Optional<CommunityPostEntity> incrementViewCount(Long postId) {
        return postEntityRepository.findWithLockingById(postId)
                .filter(post -> post.deletedAt() == null)
                .map(post -> {
                    post.incrementViewCount();
                    return post;
                });
    }

    @Transactional(readOnly = true)
    public Page<CommunityCommentEntity> findActiveComments(Long postId, Pageable pageable) {
        return commentEntityRepository.findByPostIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(postId, pageable);
    }

    @Transactional
    public CommunityCommentEntity saveCommentAndIncrementCount(CommunityCommentEntity comment) {
        CommunityPostEntity post = postEntityRepository.findWithLockingById(comment.postId())
                .filter(candidate -> candidate.deletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("active post not found"));
        CommunityCommentEntity saved = commentEntityRepository.save(comment);
        post.incrementCommentCount();
        return saved;
    }

    @Transactional
    public Optional<CommunityCommentEntity> softDeleteCommentAndDecrementCount(Long commentId, Instant deletedAt) {
        return commentEntityRepository.findWithLockingById(commentId)
                .filter(comment -> comment.deletedAt() == null)
                .flatMap(comment -> postEntityRepository.findWithLockingById(comment.postId())
                        .filter(post -> post.deletedAt() == null)
                        .map(post -> {
                            comment.softDelete(deletedAt);
                            post.decrementCommentCount();
                            return comment;
                        }));
    }

    @Transactional(readOnly = true)
    public boolean existsLike(Long postId, Long memberId) {
        return likeEntityRepository.existsByPostIdAndMemberId(postId, memberId);
    }

    @Transactional
    public LikeMutation addLikeIfAbsent(Long postId, Long memberId, Instant createdAt) {
        CommunityPostEntity post = postEntityRepository.findWithLockingById(postId)
                .filter(candidate -> candidate.deletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("active post not found"));
        if (!likeEntityRepository.existsByPostIdAndMemberId(postId, memberId)) {
            likeEntityRepository.save(new CommunityPostLikeEntity(postId, memberId, createdAt));
            post.incrementLikeCount();
            return new LikeMutation(true, post.likeCount());
        }
        return new LikeMutation(false, post.likeCount());
    }

    @Transactional
    public LikeMutation removeLikeIfPresent(Long postId, Long memberId) {
        CommunityPostEntity post = postEntityRepository.findWithLockingById(postId)
                .filter(candidate -> candidate.deletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("active post not found"));
        long deleted = likeEntityRepository.deleteByPostIdAndMemberId(postId, memberId);
        if (deleted > 0) {
            post.decrementLikeCount();
            return new LikeMutation(true, post.likeCount());
        }
        return new LikeMutation(false, post.likeCount());
    }

    @Transactional(readOnly = true)
    public List<CommunityPostImageEntity> findOwnedAttachableImages(Long uploaderMemberId, Collection<String> objectKeys) {
        Set<String> requestedKeys = new HashSet<>(objectKeys == null ? Set.of() : objectKeys);
        if (requestedKeys.isEmpty()) {
            return List.of();
        }
        String requiredPrefix = "community/" + uploaderMemberId + "/";
        return imageEntityRepository.findByObjectKeyIn(requestedKeys).stream()
                .filter(image -> uploaderMemberId.equals(image.uploaderMemberId()))
                .filter(image -> image.objectKey().startsWith(requiredPrefix))
                .filter(image -> ATTACHABLE_IMAGE_STATUSES.contains(image.status()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityPostImageEntity> findAttachedImages(Long postId) {
        return imageEntityRepository.findByPostIdAndStatus(postId, ATTACHED_STATUS);
    }

    @Transactional
    public List<CommunityPostImageEntity> attachImages(Long postId, Long uploaderMemberId, Collection<String> objectKeys) {
        List<CommunityPostImageEntity> images = findOwnedAttachableImages(uploaderMemberId, objectKeys);
        images.forEach(image -> image.attachToPost(postId));
        return images;
    }

    @Transactional
    public CommunityPostImageEntity saveImageIntent(CommunityPostImageEntity image) {
        return imageEntityRepository.save(image);
    }

    public record LikeMutation(boolean changed, long likeCount) {
    }
}
