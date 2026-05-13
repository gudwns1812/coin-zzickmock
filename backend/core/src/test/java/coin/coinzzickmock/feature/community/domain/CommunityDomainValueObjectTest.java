package coin.coinzzickmock.feature.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
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

        assertThat(image.attachableBy(7L)).isTrue();
        assertThat(image.attachTo(10L, Instant.parse("2026-05-13T00:01:00Z")).status())
                .isEqualTo(CommunityPostImageStatus.ATTACHED);
        assertThat(image.markOrphaned(Instant.parse("2026-05-13T00:02:00Z")).status())
                .isEqualTo(CommunityPostImageStatus.ORPHANED);
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
    }

    @Test
    void validatesActorAndLikeIdentifiers() {
        CommunityActor actor = new CommunityActor(9L, true);

        assertThat(actor.sameMember(9L)).isTrue();
        assertThrows(CoreException.class, () -> new CommunityActor(0L, false));
        assertThrows(CoreException.class, () -> new CommunityLike(0L, 1L, Instant.now()));
    }
}
