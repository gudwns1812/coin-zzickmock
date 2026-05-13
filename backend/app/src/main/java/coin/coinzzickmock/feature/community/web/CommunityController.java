package coin.coinzzickmock.feature.community.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.command.CreateCommunityCommentCommand;
import coin.coinzzickmock.feature.community.application.command.CreateCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.command.DeleteCommunityCommentCommand;
import coin.coinzzickmock.feature.community.application.command.DeleteCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.command.GenerateCommunityImageUploadPresignedUrlCommand;
import coin.coinzzickmock.feature.community.application.command.ToggleCommunityPostLikeCommand;
import coin.coinzzickmock.feature.community.application.command.UpdateCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.query.GetCommunityPostQuery;
import coin.coinzzickmock.feature.community.application.query.ListCommunityCommentsQuery;
import coin.coinzzickmock.feature.community.application.query.ListCommunityPostsQuery;
import coin.coinzzickmock.feature.community.application.result.CommunityCommentListResult;
import coin.coinzzickmock.feature.community.application.result.CommunityCommentMutationResult;
import coin.coinzzickmock.feature.community.application.result.CommunityCommentResult;
import coin.coinzzickmock.feature.community.application.result.CommunityImageUploadPresignedUrlResult;
import coin.coinzzickmock.feature.community.application.result.CommunityLikeResult;
import coin.coinzzickmock.feature.community.application.result.CommunityPostDetailResult;
import coin.coinzzickmock.feature.community.application.result.CommunityPostListResult;
import coin.coinzzickmock.feature.community.application.result.CommunityPostMutationResult;
import coin.coinzzickmock.feature.community.application.result.CommunityPostSummaryResult;
import coin.coinzzickmock.feature.community.application.service.CreateCommunityCommentService;
import coin.coinzzickmock.feature.community.application.service.CreateCommunityPostService;
import coin.coinzzickmock.feature.community.application.service.DeleteCommunityCommentService;
import coin.coinzzickmock.feature.community.application.service.DeleteCommunityPostService;
import coin.coinzzickmock.feature.community.application.service.GenerateCommunityImageUploadPresignedUrlService;
import coin.coinzzickmock.feature.community.application.service.GetCommunityPostService;
import coin.coinzzickmock.feature.community.application.service.ListCommunityCommentsService;
import coin.coinzzickmock.feature.community.application.service.ListCommunityPostsService;
import coin.coinzzickmock.feature.community.application.service.ToggleCommunityPostLikeService;
import coin.coinzzickmock.feature.community.application.service.UpdateCommunityPostService;
import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.content.TiptapContentPolicy;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/futures/community")
public class CommunityController {
    private final ListCommunityPostsService listCommunityPostsService;
    private final GetCommunityPostService getCommunityPostService;
    private final CreateCommunityPostService createCommunityPostService;
    private final UpdateCommunityPostService updateCommunityPostService;
    private final DeleteCommunityPostService deleteCommunityPostService;
    private final ListCommunityCommentsService listCommunityCommentsService;
    private final CreateCommunityCommentService createCommunityCommentService;
    private final DeleteCommunityCommentService deleteCommunityCommentService;
    private final ToggleCommunityPostLikeService toggleCommunityPostLikeService;
    private final GenerateCommunityImageUploadPresignedUrlService generateImageUploadPresignedUrlService;
    private final Providers providers;
    private final ObjectMapper objectMapper;
    private final List<String> allowedImageSrcPrefixes;

    public CommunityController(
            ListCommunityPostsService listCommunityPostsService,
            GetCommunityPostService getCommunityPostService,
            CreateCommunityPostService createCommunityPostService,
            UpdateCommunityPostService updateCommunityPostService,
            DeleteCommunityPostService deleteCommunityPostService,
            ListCommunityCommentsService listCommunityCommentsService,
            CreateCommunityCommentService createCommunityCommentService,
            DeleteCommunityCommentService deleteCommunityCommentService,
            ToggleCommunityPostLikeService toggleCommunityPostLikeService,
            GenerateCommunityImageUploadPresignedUrlService generateImageUploadPresignedUrlService,
            Providers providers,
            ObjectMapper objectMapper,
            @Value("${coin.community.images.allowed-src-prefixes:https://}") String allowedImageSrcPrefixes
    ) {
        this.listCommunityPostsService = listCommunityPostsService;
        this.getCommunityPostService = getCommunityPostService;
        this.createCommunityPostService = createCommunityPostService;
        this.updateCommunityPostService = updateCommunityPostService;
        this.deleteCommunityPostService = deleteCommunityPostService;
        this.listCommunityCommentsService = listCommunityCommentsService;
        this.createCommunityCommentService = createCommunityCommentService;
        this.deleteCommunityCommentService = deleteCommunityCommentService;
        this.toggleCommunityPostLikeService = toggleCommunityPostLikeService;
        this.generateImageUploadPresignedUrlService = generateImageUploadPresignedUrlService;
        this.providers = providers;
        this.objectMapper = objectMapper;
        this.allowedImageSrcPrefixes = Arrays.stream(allowedImageSrcPrefixes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    @GetMapping("/posts")
    public ApiResponse<CommunityPostListResponse> posts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        providers.auth().currentActor();
        CommunityPostListResult result = listCommunityPostsService.execute(
                new ListCommunityPostsQuery(parseCategory(category, false), page, size)
        );
        return ApiResponse.success(CommunityPostListResponse.from(result));
    }

    @PostMapping("/posts")
    public ApiResponse<CommunityPostMutationResponse> createPost(@RequestBody CommunityPostUpsertRequest request) {
        Actor actor = providers.auth().currentActor();
        CommunityPostMutationResult result = createCommunityPostService.execute(new CreateCommunityPostCommand(
                actor.memberId(),
                actor.nickname(),
                actor.admin(),
                parseCategory(requireRequest(request).category(), true),
                request.title(),
                jsonText(request.contentJson()),
                request.safeImageObjectKeys(),
                contentPolicy(request.safeImageObjectKeys())
        ));
        return ApiResponse.success(CommunityPostMutationResponse.from(result));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<CommunityPostDetailResponse> post(@PathVariable Long postId) {
        Actor actor = providers.auth().currentActor();
        CommunityPostDetailResult result = getCommunityPostService.execute(
                new GetCommunityPostQuery(postId, actor.memberId(), actor.admin())
        );
        return ApiResponse.success(CommunityPostDetailResponse.from(result));
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<CommunityPostMutationResponse> updatePost(
            @PathVariable Long postId,
            @RequestBody CommunityPostUpsertRequest request
    ) {
        Actor actor = providers.auth().currentActor();
        CommunityPostMutationResult result = updateCommunityPostService.execute(new UpdateCommunityPostCommand(
                postId,
                actor.memberId(),
                actor.admin(),
                parseCategory(requireRequest(request).category(), true),
                request.title(),
                jsonText(request.contentJson()),
                request.safeImageObjectKeys(),
                contentPolicy(request.safeImageObjectKeys())
        ));
        return ApiResponse.success(CommunityPostMutationResponse.from(result));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<CommunityDeleteResponse> deletePost(@PathVariable Long postId) {
        Actor actor = providers.auth().currentActor();
        deleteCommunityPostService.execute(new DeleteCommunityPostCommand(postId, actor.memberId(), actor.admin()));
        return ApiResponse.success(new CommunityDeleteResponse(true));
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<CommunityCommentListResponse> comments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Actor actor = providers.auth().currentActor();
        CommunityCommentListResult result = listCommunityCommentsService.execute(
                new ListCommunityCommentsQuery(postId, page, size), actor.memberId(), actor.admin()
        );
        return ApiResponse.success(CommunityCommentListResponse.from(result));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommunityCommentMutationResponse> createComment(
            @PathVariable Long postId,
            @RequestBody CommunityCommentCreateRequest request
    ) {
        Actor actor = providers.auth().currentActor();
        CommunityCommentMutationResult result = createCommunityCommentService.execute(new CreateCommunityCommentCommand(
                postId,
                actor.memberId(),
                actor.nickname(),
                requireRequest(request).content()
        ));
        return ApiResponse.success(CommunityCommentMutationResponse.from(result));
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ApiResponse<CommunityDeleteResponse> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        Actor actor = providers.auth().currentActor();
        deleteCommunityCommentService.execute(new DeleteCommunityCommentCommand(postId, commentId, actor.memberId(), actor.admin()));
        return ApiResponse.success(new CommunityDeleteResponse(true));
    }

    @PostMapping("/posts/{postId}/like")
    public ApiResponse<CommunityLikeResponse> like(@PathVariable Long postId) {
        Actor actor = providers.auth().currentActor();
        CommunityLikeResult result = toggleCommunityPostLikeService.like(new ToggleCommunityPostLikeCommand(postId, actor.memberId()));
        return ApiResponse.success(CommunityLikeResponse.from(result));
    }

    @DeleteMapping("/posts/{postId}/like")
    public ApiResponse<CommunityLikeResponse> unlike(@PathVariable Long postId) {
        Actor actor = providers.auth().currentActor();
        CommunityLikeResult result = toggleCommunityPostLikeService.unlike(new ToggleCommunityPostLikeCommand(postId, actor.memberId()));
        return ApiResponse.success(CommunityLikeResponse.from(result));
    }

    @PostMapping("/images/presign")
    public ApiResponse<CommunityImageUploadPresignedUrlResponse> presignImage(
            @RequestBody CommunityImageUploadPresignRequest request
    ) {
        Actor actor = providers.auth().currentActor();
        CommunityImageUploadPresignedUrlResult result = generateImageUploadPresignedUrlService.execute(
                new GenerateCommunityImageUploadPresignedUrlCommand(
                        actor.memberId(),
                        requireRequest(request).fileName(),
                        request.contentType(),
                        request.sizeBytes()
                )
        );
        return ApiResponse.success(CommunityImageUploadPresignedUrlResponse.from(result));
    }

    private CommunityCategory parseCategory(String category, boolean required) {
        if (category == null || category.isBlank()) {
            if (required) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
            return null;
        }
        try {
            return CommunityCategory.valueOf(category.trim());
        } catch (IllegalArgumentException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String jsonText(JsonNode contentJson) {
        if (contentJson == null || contentJson.isNull()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (contentJson.isTextual()) {
            return contentJson.asText();
        }
        try {
            return objectMapper.writeValueAsString(contentJson);
        } catch (JsonProcessingException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private TiptapContentPolicy contentPolicy(Set<String> imageObjectKeys) {
        if (imageObjectKeys == null || imageObjectKeys.isEmpty()) {
            return TiptapContentPolicy.withoutImages();
        }
        return TiptapContentPolicy.withImages(imageObjectKeys, allowedImageSrcPrefixes);
    }

    private static <T> T requireRequest(T request) {
        if (request == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return request;
    }
}

record CommunityPostUpsertRequest(String category, String title, JsonNode contentJson, Set<String> imageObjectKeys) {
    Set<String> safeImageObjectKeys() {
        return imageObjectKeys == null ? Set.of() : Set.copyOf(imageObjectKeys);
    }
}

record CommunityCommentCreateRequest(String content) {
}

record CommunityImageUploadPresignRequest(String fileName, String contentType, long sizeBytes) {
}

record CommunityPostListResponse(
        List<CommunityPostSummaryResponse> pinnedNotices,
        List<CommunityPostSummaryResponse> posts,
        CommunityPageResponse page
) {
    static CommunityPostListResponse from(CommunityPostListResult result) {
        return new CommunityPostListResponse(
                result.pinnedNotices().stream().map(CommunityPostSummaryResponse::from).toList(),
                result.posts().stream().map(CommunityPostSummaryResponse::from).toList(),
                new CommunityPageResponse(result.page(), result.size(), result.totalElements(), result.totalPages(), result.hasNext())
        );
    }
}

record CommunityPostSummaryResponse(
        Long id,
        CommunityCategory category,
        String title,
        String authorNickname,
        long viewCount,
        long likeCount,
        long commentCount,
        Instant createdAt
) {
    static CommunityPostSummaryResponse from(CommunityPostSummaryResult result) {
        return new CommunityPostSummaryResponse(result.id(), result.category(), result.title(), result.authorNickname(),
                result.viewCount(), result.likeCount(), result.commentCount(), result.createdAt());
    }
}

record CommunityPostDetailResponse(
        Long id,
        CommunityCategory category,
        String title,
        String authorNickname,
        String contentJson,
        long viewCount,
        long likeCount,
        long commentCount,
        boolean canEdit,
        boolean canDelete,
        boolean likedByMe,
        Instant createdAt,
        Instant updatedAt
) {
    static CommunityPostDetailResponse from(CommunityPostDetailResult result) {
        return new CommunityPostDetailResponse(result.id(), result.category(), result.title(), result.authorNickname(),
                result.contentJson(), result.viewCount(), result.likeCount(), result.commentCount(), result.canEdit(),
                result.canDelete(), result.isLikedByMe(), result.createdAt(), result.updatedAt());
    }
}

record CommunityPageResponse(int page, int size, long totalElements, int totalPages, boolean hasNext) {
}

record CommunityPostMutationResponse(Long postId) {
    static CommunityPostMutationResponse from(CommunityPostMutationResult result) {
        return new CommunityPostMutationResponse(result.postId());
    }
}

record CommunityCommentListResponse(List<CommunityCommentResponse> comments, CommunityPageResponse page) {
    static CommunityCommentListResponse from(CommunityCommentListResult result) {
        return new CommunityCommentListResponse(
                result.comments().stream().map(CommunityCommentResponse::from).toList(),
                new CommunityPageResponse(result.page(), result.size(), result.totalElements(), result.totalPages(), result.hasNext())
        );
    }
}

record CommunityCommentResponse(
        Long id,
        Long postId,
        String authorNickname,
        String content,
        boolean canDelete,
        Instant createdAt
) {
    static CommunityCommentResponse from(CommunityCommentResult result) {
        return new CommunityCommentResponse(result.id(), result.postId(), result.authorNickname(), result.content(),
                result.canDelete(), result.createdAt());
    }
}

record CommunityCommentMutationResponse(Long commentId) {
    static CommunityCommentMutationResponse from(CommunityCommentMutationResult result) {
        return new CommunityCommentMutationResponse(result.commentId());
    }
}

record CommunityLikeResponse(Long postId, boolean likedByMe) {
    static CommunityLikeResponse from(CommunityLikeResult result) {
        return new CommunityLikeResponse(result.postId(), result.isLikedByMe());
    }
}

record CommunityDeleteResponse(boolean deleted) {
}

record CommunityImageUploadPresignedUrlResponse(
        String uploadUrl,
        String objectKey,
        String publicUrl,
        String contentType,
        Instant expiresAt,
        long maxBytes
) {
    static CommunityImageUploadPresignedUrlResponse from(CommunityImageUploadPresignedUrlResult result) {
        return new CommunityImageUploadPresignedUrlResponse(result.uploadUrl(), result.objectKey(), result.publicUrl(),
                result.contentType(), result.expiresAt(), result.maxBytes());
    }
}
