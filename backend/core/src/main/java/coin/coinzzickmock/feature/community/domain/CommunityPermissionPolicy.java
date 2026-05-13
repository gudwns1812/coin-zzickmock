package coin.coinzzickmock.feature.community.domain;

public final class CommunityPermissionPolicy {
    private CommunityPermissionPolicy() {
    }

    public static boolean canCreatePost(boolean admin, CommunityCategory category) {
        return category != null && (!category.isNotice() || admin);
    }

    public static boolean canEditPost(boolean admin, boolean author, CommunityCategory currentCategory, CommunityCategory nextCategory) {
        if (currentCategory == null || nextCategory == null) {
            return false;
        }
        if (nextCategory.isNotice() && !admin) {
            return false;
        }
        return admin || author;
    }

    public static boolean canDeletePost(boolean admin, boolean author) {
        return admin || author;
    }

    public static boolean canCreateComment(boolean authenticated) {
        return authenticated;
    }

    public static boolean canDeleteComment(boolean admin, boolean author) {
        return admin || author;
    }

    public static boolean canToggleLike(boolean authenticated) {
        return authenticated;
    }
}
