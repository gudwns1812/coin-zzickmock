package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public final class CommunityPermissionPolicy {
    public void requirePostCreatable(CommunityActor actor, CommunityPostCategory category) {
        requireActor(actor);
        requireCategory(category);
        if (category.notice() && !actor.admin()) {
            throw forbidden();
        }
    }

    public void requirePostEditable(CommunityPost post, CommunityActor actor, CommunityPostCategory requestedCategory) {
        requirePost(post);
        requireActor(actor);
        requireCategory(requestedCategory);
        if (requestedCategory.notice() && !actor.admin()) {
            throw forbidden();
        }
        if (post.category().notice() && !actor.admin()) {
            throw forbidden();
        }
        if (!actor.admin() && !post.authoredBy(actor.memberId())) {
            throw forbidden();
        }
        if (post.deleted()) {
            throw notFound();
        }
    }

    public void requirePostDeletable(CommunityPost post, CommunityActor actor) {
        requirePost(post);
        requireActor(actor);
        if (post.deleted()) {
            throw notFound();
        }
        if (!actor.admin() && !post.authoredBy(actor.memberId())) {
            throw forbidden();
        }
    }

    public void requireCommentDeletable(CommunityComment comment, CommunityActor actor) {
        if (comment == null) {
            throw notFound();
        }
        requireActor(actor);
        if (comment.deleted()) {
            throw notFound();
        }
        if (!actor.admin() && !comment.authoredBy(actor.memberId())) {
            throw forbidden();
        }
    }

    private static void requirePost(CommunityPost post) {
        if (post == null) {
            throw notFound();
        }
    }

    private static void requireActor(CommunityActor actor) {
        if (actor == null) {
            throw new CoreException(ErrorCode.UNAUTHORIZED);
        }
    }

    private static void requireCategory(CommunityPostCategory category) {
        if (category == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static CoreException forbidden() {
        return new CoreException(ErrorCode.FORBIDDEN);
    }

    private static CoreException notFound() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
