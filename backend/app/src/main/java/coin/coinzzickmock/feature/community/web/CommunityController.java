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
import coin.coinzzickmock.feature.community.application.result.CommunityImageUploadPresignedUrlResult;
import coin.coinzzickmock.feature.community.application.result.CommunityLikeResult;
import coin.coinzzickmock.feature.community.application.result.CommunityPostDetailResult;
import coin.coinzzickmock.feature.community.application.result.CommunityPostListResult;
import coin.coinzzickmock.feature.community.application.result.CommunityPostMutationResult;
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
import coin.coinzzickmock.feature.community.web.request.CommunityCommentCreateRequest;
import coin.coinzzickmock.feature.community.web.request.CommunityImageUploadPresignRequest;
import coin.coinzzickmock.feature.community.web.request.CommunityPostUpsertRequest;
import coin.coinzzickmock.feature.community.web.response.CommunityCommentListResponse;
import coin.coinzzickmock.feature.community.web.response.CommunityCommentMutationResponse;
import coin.coinzzickmock.feature.community.web.response.CommunityDeleteResponse;
import coin.coinzzickmock.feature.community.web.response.CommunityImageUploadPresignedUrlResponse;
import coin.coinzzickmock.feature.community.web.response.CommunityLikeResponse;
import coin.coinzzickmock.feature.community.web.response.CommunityPostDetailResponse;
import coin.coinzzickmock.feature.community.web.response.CommunityPostListResponse;
import coin.coinzzickmock.feature.community.web.response.CommunityPostMutationResponse;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            @Value("${coin.community.images.allowed-src-prefixes:}") String allowedImageSrcPrefixes,
            @Value("${coin.community.s3.public-base-url:}") String publicBaseUrl,
            @Value("${coin.community.s3.bucket:coin-zzickmock-community-local-672420933257-ap-southeast-2-an}") String bucket,
            @Value("${coin.community.s3.region:ap-southeast-2}") String region
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
        this.allowedImageSrcPrefixes = Arrays.stream(resolveAllowedImageSrcPrefixes(
                        allowedImageSrcPrefixes,
                        publicBaseUrl,
                        bucket,
                        region
                ).split(","))
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
                parseCategory(request.category(), true),
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
                parseCategory(request.category(), true),
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
                request.content()
        ));
        return ApiResponse.success(CommunityCommentMutationResponse.from(result));
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ApiResponse<CommunityDeleteResponse> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        Actor actor = providers.auth().currentActor();
        deleteCommunityCommentService.execute(
                new DeleteCommunityCommentCommand(postId, commentId, actor.memberId(), actor.admin())
        );
        return ApiResponse.success(new CommunityDeleteResponse(true));
    }

    @PostMapping("/posts/{postId}/like")
    public ApiResponse<CommunityLikeResponse> like(@PathVariable Long postId) {
        Actor actor = providers.auth().currentActor();
        CommunityLikeResult result = toggleCommunityPostLikeService.like(
                new ToggleCommunityPostLikeCommand(postId, actor.memberId())
        );
        return ApiResponse.success(CommunityLikeResponse.from(result));
    }

    @DeleteMapping("/posts/{postId}/like")
    public ApiResponse<CommunityLikeResponse> unlike(@PathVariable Long postId) {
        Actor actor = providers.auth().currentActor();
        CommunityLikeResult result = toggleCommunityPostLikeService.unlike(
                new ToggleCommunityPostLikeCommand(postId, actor.memberId())
        );
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
                        request.fileName(),
                        request.contentType(),
                        request.sizeBytes()
                )
        );
        return ApiResponse.success(CommunityImageUploadPresignedUrlResponse.from(result));
    }

    private CommunityCategory parseCategory(String category, boolean required) {
        if (category == null || category.isBlank()) {
            if (required) {
                throw new CoreException(ErrorCode.COMMUNITY_POST_INVALID_CATEGORY);
            }
            return null;
        }
        try {
            return CommunityCategory.valueOf(category.trim());
        } catch (IllegalArgumentException exception) {
            throw new CoreException(ErrorCode.COMMUNITY_POST_INVALID_CATEGORY);
        }
    }

    private String jsonText(JsonNode contentJson) {
        if (contentJson == null || contentJson.isNull()) {
            throw new CoreException(ErrorCode.COMMUNITY_POST_INVALID_CONTENT);
        }
        if (contentJson.isTextual()) {
            return contentJson.asText();
        }
        try {
            return objectMapper.writeValueAsString(contentJson);
        } catch (JsonProcessingException exception) {
            throw new CoreException(ErrorCode.COMMUNITY_POST_INVALID_CONTENT);
        }
    }

    private TiptapContentPolicy contentPolicy(Set<String> imageObjectKeys) {
        if (imageObjectKeys == null || imageObjectKeys.isEmpty()) {
            return TiptapContentPolicy.withoutImages();
        }
        return TiptapContentPolicy.withImages(imageObjectKeys, allowedImageSrcPrefixes);
    }

    private static String resolveAllowedImageSrcPrefixes(
            String allowedImageSrcPrefixes,
            String publicBaseUrl,
            String bucket,
            String region
    ) {
        if (allowedImageSrcPrefixes != null && !allowedImageSrcPrefixes.isBlank()) {
            return allowedImageSrcPrefixes;
        }
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl;
        }
        return "https://" + bucket.trim() + ".s3." + region.trim() + ".amazonaws.com";
    }

}
