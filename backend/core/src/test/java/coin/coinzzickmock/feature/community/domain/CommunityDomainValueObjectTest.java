package coin.coinzzickmock.feature.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CommunityDomainValueObjectTest {
    @Test
    void trimsAuthorNicknameAndRejectsBlankValues() {
        CommunityAuthor author = new CommunityAuthor(1L, "  운영자  ");

        assertThat(author.nickname()).isEqualTo("운영자");
        assertThrows(CoreException.class, () -> new CommunityAuthor(1L, "   "));
    }

    @Test
    void rejectsCountUnderflow() {
        CommunityPostCounts counts = new CommunityPostCounts(0, 1, 1);

        assertThat(counts.unliked()).isEqualTo(new CommunityPostCounts(0, 0, 1));
        assertThat(counts.uncommented()).isEqualTo(new CommunityPostCounts(0, 1, 0));
        assertThrows(CoreException.class, () -> CommunityPostCounts.zero().unliked());
        assertThrows(CoreException.class, () -> CommunityPostCounts.zero().uncommented());
    }

    @Test
    void requiresImageIntentTimestampsAndTracksAttachmentLifecycle() {
        CommunityPostImageIntent image = new CommunityPostImageIntent(
                1L,
                null,
                7L,
                "community/7/image.webp",
                "https://cdn.example/community/7/image.webp",
                "image/webp",
                1234L,
                CommunityPostImageStatus.PRESIGNED,
                Instant.parse("2026-05-13T00:00:00Z"),
                Instant.parse("2026-05-13T00:00:00Z")
        );

        assertThat(image.canBeAttachedBy(7L)).isTrue();
        CommunityPostImageIntent attached = image.attachTo(10L, Instant.parse("2026-05-13T00:01:00Z"));
        assertThat(attached.status())
                .isEqualTo(CommunityPostImageStatus.ATTACHED);
        CommunityPostImageIntent reattached = attached.attachTo(10L, Instant.parse("2026-05-13T00:02:00Z"));
        assertThat(reattached.postId()).isEqualTo(10L);
        assertThat(reattached.status()).isEqualTo(CommunityPostImageStatus.ATTACHED);
        assertThrows(CoreException.class, () -> attached.attachTo(11L, Instant.parse("2026-05-13T00:02:00Z")));
        CommunityPostImageIntent orphaned = attached.markOrphaned(Instant.parse("2026-05-13T00:03:00Z"));
        assertThat(orphaned.status())
                .isEqualTo(CommunityPostImageStatus.ORPHANED);
        assertThat(orphaned.postId()).isNull();
        assertThrows(CoreException.class, () -> orphaned.markOrphaned(Instant.parse("2026-05-13T00:04:00Z")));
        assertThrows(CoreException.class, () -> new CommunityPostImageIntent(
                1L,
                null,
                7L,
                "community/7/image.webp",
                "https://cdn.example/community/7/image.webp",
                "image/webp",
                1234L,
                CommunityPostImageStatus.PRESIGNED,
                Instant.parse("2026-05-13T00:00:00Z"),
                null
        ));
        assertThrows(CoreException.class, () -> new CommunityPostImageIntent(
                0L,
                null,
                7L,
                "community/7/image.webp",
                "https://cdn.example/community/7/image.webp",
                "image/webp",
                1234L,
                CommunityPostImageStatus.PRESIGNED,
                Instant.parse("2026-05-13T00:00:00Z"),
                Instant.parse("2026-05-13T00:00:00Z")
        ));
        assertThrows(CoreException.class, () -> new CommunityPostImageIntent(
                1L,
                0L,
                7L,
                "community/7/image.webp",
                "https://cdn.example/community/7/image.webp",
                "image/webp",
                1234L,
                CommunityPostImageStatus.ATTACHED,
                Instant.parse("2026-05-13T00:00:00Z"),
                Instant.parse("2026-05-13T00:00:00Z")
        ));
    }

    @Test
    void validatesActorAndLikeIdentifiers() {
        CommunityActor actor = new CommunityActor(9L, true);

        assertThat(actor.isAdmin()).isTrue();
        assertThat(actor.isSameMember(9L)).isTrue();
        assertThat(actor.isSameMember(null)).isFalse();
        assertThatThrownErrorCode(() -> new CommunityActor(0L, false), ErrorCode.INVALID_REQUEST);
        assertThrows(CoreException.class, () -> new CommunityLike(0L, 1L, Instant.now()));
    }

    private static void assertThatThrownErrorCode(Runnable runnable, ErrorCode errorCode) {
        CoreException exception = assertThrows(CoreException.class, runnable::run);
        assertThat(exception.errorCode()).isEqualTo(errorCode);
    }
}
