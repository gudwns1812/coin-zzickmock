package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.command.CreateCommunityCommentCommand;
import coin.coinzzickmock.feature.community.application.command.CreateCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.command.DeleteCommunityCommentCommand;
import coin.coinzzickmock.feature.community.application.command.DeleteCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.command.ToggleCommunityPostLikeCommand;
import coin.coinzzickmock.feature.community.application.command.UpdateCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.query.GetCommunityPostQuery;
import coin.coinzzickmock.feature.community.application.query.ListCommunityPostsQuery;
import coin.coinzzickmock.feature.community.application.repository.CommunityCommentPage;
import coin.coinzzickmock.feature.community.application.repository.CommunityCommentRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostImageRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostLikeRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostPage;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostListResult;
import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityComment;
import coin.coinzzickmock.feature.community.domain.CommunityImageStatus;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import coin.coinzzickmock.feature.community.domain.content.TiptapContentPolicy;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunityApplicationServiceRegressionTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC);
    private static final String CONTENT = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]}]}";

    private final InMemoryPostRepository posts = new InMemoryPostRepository();
    private final InMemoryCommentRepository comments = new InMemoryCommentRepository();
    private final InMemoryLikeRepository likes = new InMemoryLikeRepository();
    private final InMemoryImageRepository images = new InMemoryImageRepository();

    @Test
    void noticeCreateAndNoticeUpdateRemainAdminOnly() {
        CreateCommunityPostService create = new CreateCommunityPostService(posts, images, CLOCK);
        UpdateCommunityPostService update = new UpdateCommunityPostService(posts, images, CLOCK);

        assertCore(ErrorCode.FORBIDDEN, () -> create.execute(createPost(1L, false, CommunityCategory.NOTICE, CONTENT, Set.of(), TiptapContentPolicy.withoutImages())));

        Long adminNoticeId = create.execute(createPost(10L, true, CommunityCategory.NOTICE, CONTENT, Set.of(), TiptapContentPolicy.withoutImages())).postId();
        assertThat(posts.findActiveById(adminNoticeId)).map(CommunityPost::category).contains(CommunityCategory.NOTICE);

        Long normalPostId = create.execute(createPost(1L, false, CommunityCategory.CHAT, CONTENT, Set.of(), TiptapContentPolicy.withoutImages())).postId();
        assertCore(ErrorCode.FORBIDDEN, () -> update.execute(new UpdateCommunityPostCommand(
                normalPostId, 1L, false, CommunityCategory.NOTICE, "notice try", CONTENT, Set.of(), TiptapContentPolicy.withoutImages())));

        update.execute(new UpdateCommunityPostCommand(normalPostId, 99L, true, CommunityCategory.NOTICE, "admin notice", CONTENT, Set.of(), TiptapContentPolicy.withoutImages()));
        assertThat(posts.findActiveById(normalPostId)).map(CommunityPost::category).contains(CommunityCategory.NOTICE);
    }

    @Test
    void postDeleteAllowsAuthorOrAdminAndHidesSoftDeletedPostFromUseCases() {
        CreateCommunityPostService create = new CreateCommunityPostService(posts, images, CLOCK);
        DeleteCommunityPostService delete = new DeleteCommunityPostService(posts, CLOCK);
        GetCommunityPostService get = new GetCommunityPostService(posts, likes);
        ListCommunityPostsService list = new ListCommunityPostsService(posts);

        Long authorPostId = create.execute(createPost(1L, false, CommunityCategory.CHAT, CONTENT, Set.of(), TiptapContentPolicy.withoutImages())).postId();
        assertCore(ErrorCode.FORBIDDEN, () -> delete.execute(new DeleteCommunityPostCommand(authorPostId, 2L, false)));

        delete.execute(new DeleteCommunityPostCommand(authorPostId, 1L, false));
        assertCore(ErrorCode.INVALID_REQUEST, () -> get.execute(new GetCommunityPostQuery(authorPostId, 1L, false)));
        assertThat(list.execute(new ListCommunityPostsQuery(null, 0, 20)).posts()).isEmpty();

        Long adminDeletedId = create.execute(createPost(1L, false, CommunityCategory.CHAT, CONTENT, Set.of(), TiptapContentPolicy.withoutImages())).postId();
        delete.execute(new DeleteCommunityPostCommand(adminDeletedId, 99L, true));
        assertThat(posts.findActiveById(adminDeletedId)).isEmpty();
    }

    @Test
    void commentCreateTrimsAndRejectsBlankWhileDeleteAllowsCommentAuthorOrAdmin() {
        Long postId = new CreateCommunityPostService(posts, images, CLOCK)
                .execute(createPost(1L, false, CommunityCategory.CHAT, CONTENT, Set.of(), TiptapContentPolicy.withoutImages()))
                .postId();
        CreateCommunityCommentService createComment = new CreateCommunityCommentService(posts, comments, CLOCK);
        DeleteCommunityCommentService deleteComment = new DeleteCommunityCommentService(comments, CLOCK);

        assertCore(ErrorCode.INVALID_REQUEST, () -> createComment.execute(new CreateCommunityCommentCommand(postId, 2L, "commenter", "   ")));
        Long commentId = createComment.execute(new CreateCommunityCommentCommand(postId, 2L, "commenter", "  hi  ")).commentId();
        assertThat(comments.findActiveById(commentId)).map(CommunityComment::content).contains("hi");
        assertThat(posts.findActiveById(postId)).map(CommunityPost::commentCount).contains(1L);

        assertCore(ErrorCode.FORBIDDEN, () -> deleteComment.execute(new DeleteCommunityCommentCommand(postId, commentId, 3L, false)));
        deleteComment.execute(new DeleteCommunityCommentCommand(postId, commentId, 99L, true));
        assertThat(comments.findActiveById(commentId)).isEmpty();
    }

    @Test
    void likeAndUnlikeAreIdempotentAndKeepCountConsistent() {
        Long postId = new CreateCommunityPostService(posts, images, CLOCK)
                .execute(createPost(1L, false, CommunityCategory.CHAT, CONTENT, Set.of(), TiptapContentPolicy.withoutImages()))
                .postId();
        ToggleCommunityPostLikeService like = new ToggleCommunityPostLikeService(posts, likes);
        ToggleCommunityPostLikeCommand command = new ToggleCommunityPostLikeCommand(postId, 7L);

        like.like(command);
        like.like(command);
        assertThat(posts.findActiveById(postId)).map(CommunityPost::likeCount).contains(1L);

        like.unlike(command);
        like.unlike(command);
        assertThat(posts.findActiveById(postId)).map(CommunityPost::likeCount).contains(0L);
    }

    @Test
    void imageObjectKeysMustBelongToActorAndRemainAttachable() {
        CreateCommunityPostService create = new CreateCommunityPostService(posts, images, CLOCK);
        String imageJson = "{\"type\":\"doc\",\"content\":[{\"type\":\"image\",\"attrs\":{\"src\":\"https://cdn.example/community/1/chart.webp\",\"objectKey\":\"community/1/chart.webp\",\"alt\":\"chart\",\"title\":\"chart\"}}]}";
        TiptapContentPolicy policy = TiptapContentPolicy.withImages(Set.of("community/1/chart.webp"), List.of("https://cdn.example/community/"));

        assertCore(ErrorCode.INVALID_REQUEST, () -> create.execute(createPost(1L, false, CommunityCategory.CHAT, imageJson, Set.of("community/1/chart.webp"), policy)));

        images.allow(1L, "community/1/chart.webp");
        Long postId = create.execute(createPost(1L, false, CommunityCategory.CHAT, imageJson, Set.of("community/1/chart.webp"), policy)).postId();
        assertThat(images.attachedToPost(postId)).containsExactly("community/1/chart.webp");
    }

    @Test
    void listUseCaseSeparatesLatestThreeNoticesFromNormalActivePosts() {
        CreateCommunityPostService create = new CreateCommunityPostService(posts, images, CLOCK);
        Long notice1 = create.execute(createPost(99L, true, CommunityCategory.NOTICE, CONTENT, Set.of(), TiptapContentPolicy.withoutImages())).postId();
        Long notice2 = create.execute(createPost(99L, true, CommunityCategory.NOTICE, CONTENT, Set.of(), TiptapContentPolicy.withoutImages())).postId();
        Long notice3 = create.execute(createPost(99L, true, CommunityCategory.NOTICE, CONTENT, Set.of(), TiptapContentPolicy.withoutImages())).postId();
        Long notice4 = create.execute(createPost(99L, true, CommunityCategory.NOTICE, CONTENT, Set.of(), TiptapContentPolicy.withoutImages())).postId();
        Long chat = create.execute(createPost(1L, false, CommunityCategory.CHAT, CONTENT, Set.of(), TiptapContentPolicy.withoutImages())).postId();
        posts.softDelete(notice4, CLOCK.instant());

        CommunityPostListResult result = new ListCommunityPostsService(posts).execute(new ListCommunityPostsQuery(null, 0, 20));

        assertThat(result.pinnedNotices()).extracting("id").containsExactly(notice3, notice2, notice1);
        assertThat(result.posts()).extracting("id").containsExactly(chat);
    }

    private static CreateCommunityPostCommand createPost(Long actorId, boolean admin, CommunityCategory category, String content, Set<String> imageKeys, TiptapContentPolicy policy) {
        return new CreateCommunityPostCommand(actorId, "actor-" + actorId, admin, category, "title", content, imageKeys, policy);
    }

    private static void assertCore(ErrorCode errorCode, ThrowingCallable callable) {
        assertThatThrownBy(callable::call)
                .isInstanceOf(CoreException.class)
                .satisfies(error -> assertThat(((CoreException) error).errorCode()).isEqualTo(errorCode));
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }

    private static final class InMemoryPostRepository implements CommunityPostRepository {
        private final Map<Long, CommunityPost> posts = new LinkedHashMap<>();
        private long sequence;

        @Override
        public List<CommunityPost> findLatestNotices(int limit) {
            return posts.values().stream()
                    .filter(post -> post.deletedAt() == null && post.category() == CommunityCategory.NOTICE)
                    .sorted(Comparator.comparing(CommunityPost::createdAt).thenComparing(CommunityPost::id).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public CommunityPostPage findPosts(ListCommunityPostsQuery query) {
            List<CommunityPost> active = posts.values().stream()
                    .filter(post -> post.deletedAt() == null)
                    .filter(post -> post.category() != CommunityCategory.NOTICE)
                    .filter(post -> query.category() == null || post.category() == query.category())
                    .sorted(Comparator.comparing(CommunityPost::createdAt).thenComparing(CommunityPost::id).reversed())
                    .toList();
            return new CommunityPostPage(active, query.page(), query.size(), active.size(), 1, false);
        }

        @Override
        public Optional<CommunityPost> findActiveById(Long postId) {
            return Optional.ofNullable(posts.get(postId)).filter(post -> post.deletedAt() == null);
        }

        @Override
        public CommunityPost save(CommunityPost post) {
            CommunityPost saved = post.withId(++sequence);
            posts.put(saved.id(), saved);
            return saved;
        }

        @Override
        public CommunityPost update(CommunityPost post) {
            posts.put(post.id(), post.withVersion(post.version() + 1));
            return posts.get(post.id());
        }

        @Override
        public void softDelete(Long postId, Instant deletedAt) {
            posts.computeIfPresent(postId, (id, post) -> post.softDelete(deletedAt));
        }

        @Override
        public void incrementLikeCount(Long postId) {
            posts.computeIfPresent(postId, (id, post) -> post.incrementLikeCount(CLOCK.instant()));
        }

        @Override
        public void decrementLikeCount(Long postId) {
            posts.computeIfPresent(postId, (id, post) -> post.decrementLikeCount(CLOCK.instant()));
        }

        @Override
        public void incrementCommentCount(Long postId) {
            posts.computeIfPresent(postId, (id, post) -> post.incrementCommentCount(CLOCK.instant()));
        }
    }

    private static final class InMemoryCommentRepository implements CommunityCommentRepository {
        private final Map<Long, CommunityComment> comments = new HashMap<>();
        private long sequence;

        @Override
        public CommunityCommentPage findActiveByPostId(Long postId, int page, int size) {
            List<CommunityComment> active = comments.values().stream()
                    .filter(comment -> comment.deletedAt() == null && comment.postId().equals(postId))
                    .toList();
            return new CommunityCommentPage(active, page, size, active.size(), 1, false);
        }

        @Override
        public Optional<CommunityComment> findActiveById(Long commentId) {
            return Optional.ofNullable(comments.get(commentId)).filter(comment -> comment.deletedAt() == null);
        }

        @Override
        public CommunityComment save(CommunityComment comment) {
            CommunityComment saved = comment.withId(++sequence);
            comments.put(saved.id(), saved);
            return saved;
        }

        @Override
        public void softDelete(Long commentId, Instant deletedAt) {
            comments.computeIfPresent(commentId, (id, comment) -> comment.softDelete(deletedAt));
        }
    }

    private static final class InMemoryLikeRepository implements CommunityPostLikeRepository {
        private final Set<String> likes = new HashSet<>();

        @Override
        public boolean exists(Long postId, Long memberId) {
            return likes.contains(key(postId, memberId));
        }

        @Override
        public boolean addIfAbsent(Long postId, Long memberId) {
            return likes.add(key(postId, memberId));
        }

        @Override
        public boolean removeIfPresent(Long postId, Long memberId) {
            return likes.remove(key(postId, memberId));
        }

        private static String key(Long postId, Long memberId) {
            return postId + ":" + memberId;
        }
    }

    private static final class InMemoryImageRepository implements CommunityPostImageRepository {
        private final Map<Long, Set<String>> allowedByMember = new HashMap<>();
        private final Map<Long, Set<String>> attachedByPost = new HashMap<>();

        void allow(Long memberId, String objectKey) {
            allowedByMember.computeIfAbsent(memberId, ignored -> new HashSet<>()).add(objectKey);
        }

        Set<String> attachedToPost(Long postId) {
            return attachedByPost.getOrDefault(postId, Set.of());
        }

        @Override
        public Set<String> findAttachableObjectKeys(Long uploaderMemberId, Set<String> objectKeys) {
            Set<String> allowed = allowedByMember.getOrDefault(uploaderMemberId, Set.of());
            Set<String> attachable = new HashSet<>(objectKeys);
            attachable.retainAll(allowed);
            return attachable;
        }

        @Override
        public void attachToPost(Long postId, Long uploaderMemberId, Set<String> objectKeys, CommunityImageStatus status) {
            attachedByPost.computeIfAbsent(postId, ignored -> new HashSet<>()).addAll(objectKeys);
        }

        @Override
        public void detachMissingImages(Long postId, Set<String> retainedObjectKeys, CommunityImageStatus status) {
            attachedByPost.computeIfPresent(postId, (id, keys) -> {
                keys.retainAll(retainedObjectKeys);
                return keys;
            });
        }
    }
}
