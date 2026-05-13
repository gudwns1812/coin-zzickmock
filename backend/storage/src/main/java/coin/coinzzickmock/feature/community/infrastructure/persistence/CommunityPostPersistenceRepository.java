package coin.coinzzickmock.feature.community.infrastructure.persistence;

import coin.coinzzickmock.feature.community.application.query.CommunityPostListQuery;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostImageRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostLikeRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostPage;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.domain.CommunityImageStatus;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageIntent;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class CommunityPostPersistenceRepository implements CommunityPostRepository,
        CommunityPostLikeRepository, CommunityPostImageRepository {
    private static final String NOTICE_CATEGORY = "NOTICE";
    private static final String ATTACHED_STATUS = "ATTACHED";
    private static final Set<String> ATTACHABLE_IMAGE_STATUSES = Set.of("PRESIGNED", ATTACHED_STATUS);

    private final CommunityPostEntityRepository postEntityRepository;
    private final CommunityCommentEntityRepository commentEntityRepository;
    private final CommunityPostLikeEntityRepository likeEntityRepository;
    private final CommunityPostImageEntityRepository imageEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommunityPost> findLatestNotices(int limit) {
        return postEntityRepository.findTop3ByCategoryAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(NOTICE_CATEGORY)
                .stream().limit(limit).map(CommunityPostEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityPostPage findNormalPosts(CommunityPostListQuery query) {
        var pageable = PageRequest.of(query.page(), query.size());
        var page = query.category() == null || query.category().name().equals(NOTICE_CATEGORY)
                ? postEntityRepository.findByCategoryNotAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(NOTICE_CATEGORY, pageable)
                : postEntityRepository.findByCategoryAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(query.category().name(), pageable);
        return new CommunityPostPage(page.getContent().stream().map(CommunityPostEntity::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommunityPost> findActiveById(Long postId) {
        return postEntityRepository.findByIdAndDeletedAtIsNull(postId).map(CommunityPostEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<CommunityPost> findActiveByIdForUpdate(Long postId) {
        return postEntityRepository.findWithLockingById(postId).filter(post -> post.deletedAt() == null).map(CommunityPostEntity::toDomain);
    }

    @Override
    @Transactional
    public CommunityPost save(CommunityPost post) {
        CommunityPostEntity entity = post.id() == null ? CommunityPostEntity.from(post)
                : postEntityRepository.findById(post.id()).map(existing -> { existing.apply(post); return existing; })
                .orElseGet(() -> CommunityPostEntity.from(post));
        return postEntityRepository.save(entity).toDomain();
    }

    @Override @Transactional public void incrementViewCount(Long postId) { postEntityRepository.findWithLockingById(postId).ifPresent(CommunityPostEntity::incrementViewCount); }
    @Override @Transactional public void incrementLikeCount(Long postId) { postEntityRepository.findWithLockingById(postId).ifPresent(CommunityPostEntity::incrementLikeCount); }
    @Override @Transactional public void decrementLikeCount(Long postId) { postEntityRepository.findWithLockingById(postId).ifPresent(CommunityPostEntity::decrementLikeCount); }
    @Override @Transactional public void incrementCommentCount(Long postId) { postEntityRepository.findWithLockingById(postId).ifPresent(CommunityPostEntity::incrementCommentCount); }
    @Override @Transactional public void decrementCommentCount(Long postId) { postEntityRepository.findWithLockingById(postId).ifPresent(CommunityPostEntity::decrementCommentCount); }
    @Override @Transactional public void softDelete(Long postId, Instant deletedAt) { postEntityRepository.findWithLockingById(postId).ifPresent(post -> post.softDelete(deletedAt)); }

    @Override @Transactional(readOnly = true) public boolean exists(Long postId, Long memberId) { return likeEntityRepository.existsByPostIdAndMemberId(postId, memberId); }
    @Override @Transactional public boolean addIfAbsent(Long postId, Long memberId) { if (exists(postId, memberId)) return false; likeEntityRepository.save(new CommunityPostLikeEntity(postId, memberId, Instant.now())); return true; }
    @Override @Transactional public boolean removeIfPresent(Long postId, Long memberId) { return likeEntityRepository.deleteByPostIdAndMemberId(postId, memberId) > 0; }

    @Override @Transactional(readOnly = true) public Set<String> findAttachableObjectKeys(Long uploaderMemberId, Set<String> objectKeys) { return findOwnedAttachable(uploaderMemberId, objectKeys).stream().map(CommunityPostImageIntent::objectKey).collect(java.util.stream.Collectors.toSet()); }
    @Override @Transactional public void attachToPost(Long postId, Long uploaderMemberId, Set<String> objectKeys, CommunityImageStatus status) { attachImagesToPost(postId, uploaderMemberId, objectKeys, Instant.now()); }
    @Override @Transactional public void detachMissingImages(Long postId, Set<String> retainedObjectKeys, CommunityImageStatus status) { Set<String> retained = retainedObjectKeys == null ? Set.of() : retainedObjectKeys; imageEntityRepository.findByPostIdAndStatus(postId, ATTACHED_STATUS).stream().filter(image -> !retained.contains(image.objectKey())).forEach(CommunityPostImageEntity::markOrphaned); }

    @Transactional(readOnly = true)
    public List<CommunityPostImageIntent> findOwnedAttachable(Long uploaderMemberId, Collection<String> objectKeys) {
        Set<String> requested = new HashSet<>(objectKeys == null ? Set.of() : objectKeys);
        if (requested.isEmpty()) return List.of();
        String requiredPrefix = "community/" + uploaderMemberId + "/";
        return imageEntityRepository.findByObjectKeyIn(requested).stream()
                .filter(image -> uploaderMemberId.equals(image.uploaderMemberId()))
                .filter(image -> image.objectKey().startsWith(requiredPrefix))
                .filter(image -> ATTACHABLE_IMAGE_STATUSES.contains(image.status()))
                .map(CommunityPostImageEntity::toDomain).toList();
    }

    @Transactional
    public List<CommunityPostImageIntent> attachImagesToPost(Long postId, Long uploaderMemberId, Collection<String> objectKeys, Instant attachedAt) {
        Set<String> requested = new HashSet<>(objectKeys == null ? Set.of() : objectKeys);
        if (requested.isEmpty()) return List.of();
        return imageEntityRepository.findByObjectKeyIn(requested).stream()
                .filter(image -> uploaderMemberId.equals(image.uploaderMemberId()))
                .peek(image -> image.attachToPost(postId))
                .map(CommunityPostImageEntity::toDomain).toList();
    }
}
