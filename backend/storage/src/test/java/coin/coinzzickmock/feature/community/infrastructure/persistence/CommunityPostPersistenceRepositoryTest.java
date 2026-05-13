package coin.coinzzickmock.feature.community.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.domain.CommunityImageStatus;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageStatus;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class CommunityPostPersistenceRepositoryTest {
    private CommunityPostEntityRepository postEntityRepository;
    private CommunityCommentEntityRepository commentEntityRepository;
    private CommunityPostLikeEntityRepository likeEntityRepository;
    private CommunityPostImageEntityRepository imageEntityRepository;
    private CommunityPostPersistenceRepository repository;

    @BeforeEach
    void setUp() {
        postEntityRepository = mock(CommunityPostEntityRepository.class);
        commentEntityRepository = mock(CommunityCommentEntityRepository.class);
        likeEntityRepository = mock(CommunityPostLikeEntityRepository.class);
        imageEntityRepository = mock(CommunityPostImageEntityRepository.class);
        repository = new CommunityPostPersistenceRepository(
                postEntityRepository,
                commentEntityRepository,
                likeEntityRepository,
                imageEntityRepository
        );
    }

    @Test
    void updateThrowsWhenActivePostMissing() {
        when(postEntityRepository.findWithLockingByIdAndDeletedAtIsNull(1L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> repository.incrementLikeCount(1L))
                .isInstanceOfSatisfying(CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void addIfAbsentReliesOnUniqueConstraint() {
        when(likeEntityRepository.saveAndFlush(any(CommunityPostLikeEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(likeEntityRepository.existsByPostIdAndMemberId(1L, 2L)).thenReturn(true);

        assertThat(repository.addIfAbsent(1L, 2L)).isFalse();
    }

    @Test
    void findLatestNoticesUsesRequestedDatabaseLimit() {
        when(postEntityRepository.findByCategoryAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                "NOTICE",
                PageRequest.of(0, 1)
        )).thenReturn(new PageImpl<>(List.of()));

        assertThat(repository.findLatestNotices(1)).isEmpty();

        verify(postEntityRepository).findByCategoryAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                "NOTICE",
                PageRequest.of(0, 1)
        );
    }

    @Test
    void attachToPostRejectsImagesThatAreNotAttachable() {
        CommunityPostImageEntity orphaned = image("community/10/orphan.png", 10L, CommunityPostImageStatus.ORPHANED);
        when(imageEntityRepository.findByObjectKeyIn(Set.of("community/10/orphan.png")))
                .thenReturn(List.of(orphaned));

        assertThatThrownBy(() -> repository.attachToPost(
                1L,
                10L,
                Set.of("community/10/orphan.png"),
                CommunityImageStatus.ATTACHED
        )).isInstanceOf(CoreException.class);

        assertThat(orphaned.postId()).isNull();
    }

    @Test
    void attachToPostRejectsImagesWithWrongObjectKeyPrefix() {
        CommunityPostImageEntity image = image("community/11/wrong.png", 10L, CommunityPostImageStatus.PRESIGNED);
        when(imageEntityRepository.findByObjectKeyIn(Set.of("community/11/wrong.png")))
                .thenReturn(List.of(image));

        assertThatThrownBy(() -> repository.attachToPost(
                1L,
                10L,
                Set.of("community/11/wrong.png"),
                CommunityImageStatus.ATTACHED
        )).isInstanceOf(CoreException.class);

        assertThat(image.postId()).isNull();
    }

    @Test
    void attachToPostAttachesOwnedAttachableImages() {
        CommunityPostImageEntity image = image("community/10/ok.png", 10L, CommunityPostImageStatus.PRESIGNED);
        setAuditTimestamps(image);
        when(imageEntityRepository.findByObjectKeyIn(Set.of("community/10/ok.png")))
                .thenReturn(List.of(image));

        repository.attachToPost(1L, 10L, Set.of("community/10/ok.png"), CommunityImageStatus.ATTACHED);

        assertThat(image.postId()).isEqualTo(1L);
        assertThat(image.status()).isEqualTo(CommunityPostImageStatus.ATTACHED.name());
    }

    @Test
    void findLatestNoticesDoesNotQueryForNonPositiveLimit() {
        assertThat(repository.findLatestNotices(0)).isEmpty();

        verify(postEntityRepository, never()).findByCategoryAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(any(), any());
    }

    private static CommunityPostImageEntity image(
            String objectKey,
            Long uploaderMemberId,
            CommunityPostImageStatus status
    ) {
        return new CommunityPostImageEntity(
                null,
                null,
                uploaderMemberId,
                objectKey,
                "https://cdn.example.com/" + objectKey,
                "image/png",
                100L,
                status.name()
        );
    }

    private static void setAuditTimestamps(CommunityPostImageEntity image) {
        setAuditTimestamp(image, "createdAt");
        setAuditTimestamp(image, "updatedAt");
    }

    private static void setAuditTimestamp(CommunityPostImageEntity image, String fieldName) {
        try {
            Field field = AuditableEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(image, Instant.parse("2026-05-13T00:00:00Z"));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
