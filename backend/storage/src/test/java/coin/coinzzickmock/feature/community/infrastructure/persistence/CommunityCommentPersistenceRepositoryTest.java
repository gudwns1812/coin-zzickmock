package coin.coinzzickmock.feature.community.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.domain.CommunityComment;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommunityCommentPersistenceRepositoryTest {
    private CommunityCommentEntityRepository commentEntityRepository;
    private CommunityPostRepositorySupport postRepositorySupport;
    private CommunityCommentPersistenceRepository repository;

    @BeforeEach
    void setUp() {
        commentEntityRepository = mock(CommunityCommentEntityRepository.class);
        postRepositorySupport = mock(CommunityPostRepositorySupport.class);
        repository = new CommunityCommentPersistenceRepository(commentEntityRepository, postRepositorySupport);
    }

    @Test
    void saveWithExistingIdThrowsWhenCommentMissing() {
        when(commentEntityRepository.findById(99L)).thenReturn(Optional.empty());
        CommunityComment comment = CommunityComment.create(
                1L,
                2L,
                "author",
                "content",
                Instant.parse("2026-05-13T00:00:00Z")
        ).withId(99L);

        assertThatThrownBy(() -> repository.save(comment))
                .isInstanceOfSatisfying(CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void softDeleteThrowsWhenActiveCommentMissing() {
        when(commentEntityRepository.findWithLockingByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.softDelete(99L, Instant.parse("2026-05-13T00:00:00Z")))
                .isInstanceOf(CoreException.class);

        verify(postRepositorySupport, never()).decrementCommentCount(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void applyDoesNotMoveCommentOwnership() {
        CommunityCommentEntity entity = new CommunityCommentEntity(
                1L,
                10L,
                20L,
                "before",
                "before content",
                null
        );
        CommunityComment incoming = CommunityComment.create(
                30L,
                40L,
                "after",
                "after content",
                Instant.parse("2026-05-13T00:00:00Z")
        ).withId(1L);

        entity.apply(incoming);

        assertThat(entity.postId()).isEqualTo(10L);
        assertThat(entity.authorMemberId()).isEqualTo(20L);
        assertThat(entity.authorNickname()).isEqualTo("after");
        assertThat(entity.content()).isEqualTo("after content");
    }
}
