package coin.coinzzickmock.feature.community.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.community.application.command.GenerateCommunityImageUploadPresignedUrlCommand;
import coin.coinzzickmock.feature.community.application.result.CommunityImageUploadPresignedUrlResult;
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
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommunityControllerTest {
    private final ListCommunityPostsService listPosts = mock(ListCommunityPostsService.class);
    private final GetCommunityPostService getPost = mock(GetCommunityPostService.class);
    private final CreateCommunityPostService createPost = mock(CreateCommunityPostService.class);
    private final UpdateCommunityPostService updatePost = mock(UpdateCommunityPostService.class);
    private final DeleteCommunityPostService deletePost = mock(DeleteCommunityPostService.class);
    private final ListCommunityCommentsService listComments = mock(ListCommunityCommentsService.class);
    private final CreateCommunityCommentService createComment = mock(CreateCommunityCommentService.class);
    private final DeleteCommunityCommentService deleteComment = mock(DeleteCommunityCommentService.class);
    private final ToggleCommunityPostLikeService likes = mock(ToggleCommunityPostLikeService.class);
    private final GenerateCommunityImageUploadPresignedUrlService presign = mock(GenerateCommunityImageUploadPresignedUrlService.class);
    private final Providers providers = mock(Providers.class);
    private final AuthProvider auth = mock(AuthProvider.class);

    @Test
    void presignImageUsesAuthenticatedActorAndMapsResponse() {
        when(providers.auth()).thenReturn(auth);
        when(auth.currentActor()).thenReturn(new Actor(7L, "user", "user@example.com", "tester"));
        when(presign.execute(any(GenerateCommunityImageUploadPresignedUrlCommand.class))).thenReturn(
                new CommunityImageUploadPresignedUrlResult(
                        "community/7/abc.webp",
                        "https://s3.example/upload",
                        "https://cdn.example/community/7/abc.webp",
                        "image/webp",
                        Instant.parse("2026-05-13T00:10:00Z"),
                        5_242_880L
                )
        );
        CommunityController controller = controller();

        ApiResponse<CommunityImageUploadPresignedUrlResponse> response = controller.presignImage(
                new CommunityImageUploadPresignRequest("chart.webp", "image/webp", 123L)
        );

        assertThat(response.success()).isTrue();
        assertThat(response.data().objectKey()).isEqualTo("community/7/abc.webp");
        assertThat(response.data().uploadUrl()).isEqualTo("https://s3.example/upload");
        assertThat(response.data().maxBytes()).isEqualTo(5_242_880L);
        ArgumentCaptor<GenerateCommunityImageUploadPresignedUrlCommand> captor = ArgumentCaptor.forClass(
                GenerateCommunityImageUploadPresignedUrlCommand.class
        );
        verify(presign).execute(captor.capture());
        assertThat(captor.getValue().actorMemberId()).isEqualTo(7L);
        assertThat(captor.getValue().fileName()).isEqualTo("chart.webp");
        assertThat(captor.getValue().contentType()).isEqualTo("image/webp");
        assertThat(captor.getValue().contentLength()).isEqualTo(123L);
    }

    private CommunityController controller() {
        return new CommunityController(
                listPosts,
                getPost,
                createPost,
                updatePost,
                deletePost,
                listComments,
                createComment,
                deleteComment,
                likes,
                presign,
                providers,
                new ObjectMapper(),
                "https://cdn.example/"
        );
    }
}
