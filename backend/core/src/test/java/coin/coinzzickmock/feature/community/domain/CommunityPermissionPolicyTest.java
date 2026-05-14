package coin.coinzzickmock.feature.community.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommunityPermissionPolicyTest {
    @Test
    void allowsNoticesOnlyForAdmins() {
        assertThat(CommunityPermissionPolicy.canCreatePost(false, CommunityCategory.NOTICE)).isFalse();
        assertThat(CommunityPermissionPolicy.canCreatePost(true, CommunityCategory.NOTICE)).isTrue();
    }

    @Test
    void allowsNormalPostEditingForAuthorOrAdmin() {
        assertThat(CommunityPermissionPolicy.canEditPost(false, true, CommunityCategory.CHAT, CommunityCategory.CHART_ANALYSIS)).isTrue();
        assertThat(CommunityPermissionPolicy.canEditPost(false, false, CommunityCategory.CHAT, CommunityCategory.CHART_ANALYSIS)).isFalse();
        assertThat(CommunityPermissionPolicy.canEditPost(true, false, CommunityCategory.CHAT, CommunityCategory.NOTICE)).isTrue();
        assertThat(CommunityPermissionPolicy.canEditPost(false, true, CommunityCategory.CHAT, CommunityCategory.NOTICE)).isFalse();
    }

    @Test
    void allowsCommentsLikesAndDeletesOnlyForAuthenticatedUsersOrOwners() {
        assertThat(CommunityPermissionPolicy.canCreateComment(false)).isFalse();
        assertThat(CommunityPermissionPolicy.canCreateComment(true)).isTrue();
        assertThat(CommunityPermissionPolicy.canToggleLike(false)).isFalse();
        assertThat(CommunityPermissionPolicy.canToggleLike(true)).isTrue();
        assertThat(CommunityPermissionPolicy.canDeleteComment(false, false)).isFalse();
        assertThat(CommunityPermissionPolicy.canDeleteComment(false, true)).isTrue();
        assertThat(CommunityPermissionPolicy.canDeleteComment(true, false)).isTrue();
        assertThat(CommunityPermissionPolicy.canDeletePost(false, true)).isTrue();
        assertThat(CommunityPermissionPolicy.canDeletePost(true, false)).isTrue();
    }
}
