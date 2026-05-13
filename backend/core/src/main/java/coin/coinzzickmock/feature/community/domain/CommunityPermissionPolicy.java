package coin.coinzzickmock.feature.community.domain;

public final class CommunityPermissionPolicy {
    private CommunityPermissionPolicy() {
    }

    public static boolean canCreatePost(boolean isAdmin, CommunityCategory category) {
        return category != null && (!category.isNotice() || isAdmin);
    }

    public static boolean canEditPost(boolean isAdmin, boolean isAuthor, CommunityCategory currentCategory, CommunityCategory nextCategory) {
        if (currentCategory == null || nextCategory == null) {
            return false;
        }
        if (nextCategory.isNotice() && !isAdmin) {
            return false;
        }
        return isAdmin || isAuthor;
    }

    public static boolean canDeletePost(boolean isAdmin, boolean isAuthor) {
        return isAdmin || isAuthor;
    }

    public static boolean canCreateComment(boolean isAuthenticated) {
        return isAuthenticated;
    }

    public static boolean canDeleteComment(boolean isAdmin, boolean isAuthor) {
        return isAdmin || isAuthor;
    }

    public static boolean canToggleLike(boolean isAuthenticated) {
        return isAuthenticated;
    }
}
