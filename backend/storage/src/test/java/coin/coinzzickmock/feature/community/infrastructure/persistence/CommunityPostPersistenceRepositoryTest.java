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
import coin.coinzzickmock.feature.community.application.dto.CommunityPostCountDelta;
import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityImageStatus;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageIntent;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageStatus;
import coin.coinzzickmock.feature.community.domain.TiptapJsonDocument;
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
    private CommunityPostLikeEntityRepository likeEntityRepository;
    private CommunityPostImageEntityRepository imageEntityRepository;
    private CommunityPostPersistenceRepository repository;

    @BeforeEach
    void setUp() {
        postEntityRepository = mock(CommunityPostEntityRepository.class);
        likeEntityRepository = mock(CommunityPostLikeEntityRepository.class);
        imageEntityRepository = mock(CommunityPostImageEntityRepository.class);
        repository = new CommunityPostPersistenceRepository(
                postEntityRepository,
                likeEntityRepository,
                imageEntityRepository
        );
    }

    @Test
    void countDeltaApplyIgnoresMissingActivePost() {
        when(postEntityRepository.applyCountDeltaIfActive(1L, 1, 0)).thenReturn(0);

        repository.applyCountDeltas(List.of(new CommunityPostCountDelta(1L, 1, 0)));

        verify(postEntityRepository).applyCountDeltaIfActive(1L, 1, 0);
    }

    @Test
    void counterMutationsUseAtomicDeltaQueriesInsteadOfPessimisticPostLocks() {
        when(postEntityRepository.incrementViewCountIfActive(1L)).thenReturn(1);
        when(postEntityRepository.applyCountDeltaIfActive(1L, 1, -1)).thenReturn(1);

        repository.incrementViewCount(1L);
        repository.applyCountDeltas(List.of(new CommunityPostCountDelta(1L, 1, -1)));

        verify(postEntityRepository).incrementViewCountIfActive(1L);
        verify(postEntityRepository).applyCountDeltaIfActive(1L, 1, -1);
        verify(postEntityRepository, never()).findWithLockingByIdAndDeletedAtIsNull(1L);
    }

    @Test
    void updatePreservesExistingCountersAgainstStaleIncomingDomain() {
        CommunityPostEntity entity = new CommunityPostEntity(
                1L,
                10L,
                "writer",
                "CHAT",
                "old title",
                document("old body"),
                9,
                4,
                3,
                null
        );
        setAuditTimestamps(entity);
        CommunityPost staleIncoming = CommunityPost.restore(
                1L,
                10L,
                "writer",
                CommunityCategory.CHAT,
                "new title",
                TiptapJsonDocument.restore(document("new body")),
                0,
                0,
                0,
                null,
                Instant.parse("2026-05-13T00:00:00Z"),
                Instant.parse("2026-05-13T00:01:00Z"),
                0
        );
        when(postEntityRepository.findWithLockingByIdAndDeletedAtIsNull(1L))
                .thenReturn(java.util.Optional.of(entity));
        when(postEntityRepository.save(entity)).thenReturn(entity);

        CommunityPost saved = repository.update(staleIncoming);

        assertThat(saved.title()).isEqualTo("new title");
        assertThat(saved.content().value()).contains("new body");
        assertThat(saved.viewCount()).isEqualTo(9);
        assertThat(saved.likeCount()).isEqualTo(4);
        assertThat(saved.commentCount()).isEqualTo(3);
    }

    @Test
    void addIfAbsentUsesInsertAffectedRowsToAvoidDuplicateCountDeltas() {
        when(likeEntityRepository.insertIgnore(any(), any(), any())).thenReturn(0);

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
    void attachToPostRejectsImagesOwnedByAnotherMember() {
        CommunityPostImageEntity image = image("community/11/wrong.png", 11L, CommunityPostImageStatus.PRESIGNED);
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
    void attachToPostAllowsConfiguredNonCommunityObjectKeyPrefix() {
        CommunityPostImageEntity image = image("uploads/10/ok.png", 10L, CommunityPostImageStatus.PRESIGNED);
        setAuditTimestamps(image);
        when(imageEntityRepository.findByObjectKeyIn(Set.of("uploads/10/ok.png")))
                .thenReturn(List.of(image));

        repository.attachToPost(1L, 10L, Set.of("uploads/10/ok.png"), CommunityImageStatus.ATTACHED);

        assertThat(image.postId()).isEqualTo(1L);
        assertThat(image.status()).isEqualTo(CommunityPostImageStatus.ATTACHED.name());
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
    void saveImageIntentPersistsPresignedImage() {
        CommunityPostImageIntent intent = new CommunityPostImageIntent(
                null,
                null,
                10L,
                "community/10/new.png",
                "https://cdn.example.com/community/10/new.png",
                "image/png",
                100L,
                CommunityPostImageStatus.PRESIGNED,
                Instant.parse("2026-05-13T00:00:00Z"),
                Instant.parse("2026-05-13T00:00:00Z")
        );

        repository.saveImageIntent(intent);

        verify(imageEntityRepository).saveAndFlush(any(CommunityPostImageEntity.class));
    }

    @Test
    void saveImageIntentTranslatesDuplicateObjectKey() {
        when(imageEntityRepository.saveAndFlush(any(CommunityPostImageEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        CommunityPostImageIntent intent = new CommunityPostImageIntent(
                null,
                null,
                10L,
                "community/10/new.png",
                "https://cdn.example.com/community/10/new.png",
                "image/png",
                100L,
                CommunityPostImageStatus.PRESIGNED,
                Instant.parse("2026-05-13T00:00:00Z"),
                Instant.parse("2026-05-13T00:00:00Z")
        );

        assertThatThrownBy(() -> repository.saveImageIntent(intent)).isInstanceOf(CoreException.class);
    }

    @Test
    void postEntityRestoresPersistedImageContentWithoutUploadPolicy() {
        String imageContent = """
                {"type":"doc","content":[
                  {"type":"image","attrs":{"src":"https://cdn.example.com/community/10/ok.png","objectKey":"community/10/ok.png","alt":"ok.png"}}
                ]}
                """;
        CommunityPostEntity entity = new CommunityPostEntity(
                1L,
                10L,
                "writer",
                "CHAT",
                "image post",
                imageContent,
                0,
                0,
                0,
                null
        );
        setAuditTimestamps(entity);

        CommunityPost post = entity.toDomain();

        assertThat(post.content().value()).contains("\"objectKey\":\"community/10/ok.png\"");
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

    private static String document(String text) {
        return """
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"%s"}]}]}
                """.formatted(text);
    }

    private static void setAuditTimestamps(Object entity) {
        setAuditTimestamp(entity, "createdAt");
        setAuditTimestamp(entity, "updatedAt");
    }

    private static void setAuditTimestamp(Object entity, String fieldName) {
        try {
            Field field = AuditableEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, Instant.parse("2026-05-13T00:00:00Z"));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
