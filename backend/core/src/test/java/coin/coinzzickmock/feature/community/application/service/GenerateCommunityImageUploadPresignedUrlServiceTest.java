package coin.coinzzickmock.feature.community.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coin.coinzzickmock.feature.community.application.CommunityImageUploadSettings;
import coin.coinzzickmock.feature.community.application.command.GenerateCommunityImageUploadPresignedUrlCommand;
import coin.coinzzickmock.feature.community.application.gateway.CommunityImageUploadGateway;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostImageRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityImageUploadPresignedUrlResult;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageIntent;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageStatus;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateCommunityImageUploadPresignedUrlServiceTest {
    @Mock
    private CommunityImageUploadGateway uploadGateway;
    @Mock
    private CommunityPostImageRepository imageRepository;

    private GenerateCommunityImageUploadPresignedUrlService service;

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-13T10:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        service = new GenerateCommunityImageUploadPresignedUrlService(uploadGateway, imageRepository, CommunityImageUploadSettings.defaults(), clock);
    }

    @Test
    void generatesPresignedUrlAndSavesIntent() throws MalformedURLException {
        // given
        Long memberId = 1L;
        String contentType = "image/png";
        long contentLength = 1024L;
        GenerateCommunityImageUploadPresignedUrlCommand command = new GenerateCommunityImageUploadPresignedUrlCommand(
                memberId, "chart.png", contentType, contentLength
        );

        URL expectedUrl = new URL("https://s3.example.com/presigned-put-url");
        given(uploadGateway.generatePresignedPutUrl(any(String.class), eq(contentType), eq(contentLength), any(Duration.class)))
                .willReturn(expectedUrl);
        given(uploadGateway.getPublicUrl(any(String.class)))
                .willAnswer(invocation -> "https://cdn.example.com/" + invocation.getArgument(0));

        // when
        CommunityImageUploadPresignedUrlResult result = service.execute(command);

        // then
        assertThat(result.uploadUrl()).isEqualTo(expectedUrl.toString());
        assertThat(result.publicUrl()).startsWith("https://cdn.example.com/community/1/");
        assertThat(result.objectKey()).startsWith("community/1/");
        assertThat(result.objectKey()).endsWith(".png");
        assertThat(result.contentType()).isEqualTo(contentType);
        assertThat(result.expiresAt()).isEqualTo(Instant.now(clock).plus(CommunityImageUploadSettings.DEFAULT_PRESIGN_TTL));
        assertThat(result.maxBytes()).isEqualTo(CommunityImageUploadSettings.DEFAULT_MAX_BYTES);

        ArgumentCaptor<CommunityPostImageIntent> intentCaptor = ArgumentCaptor.forClass(CommunityPostImageIntent.class);
        verify(imageRepository).saveImageIntent(intentCaptor.capture());

        CommunityPostImageIntent savedIntent = intentCaptor.getValue();
        assertThat(savedIntent.uploaderMemberId()).isEqualTo(memberId);
        assertThat(savedIntent.objectKey()).isEqualTo(result.objectKey());
        assertThat(savedIntent.publicUrl()).isEqualTo(result.publicUrl());
        assertThat(savedIntent.contentType()).isEqualTo(contentType);
        assertThat(savedIntent.sizeBytes()).isEqualTo(contentLength);
        assertThat(savedIntent.status()).isEqualTo(CommunityPostImageStatus.PRESIGNED);
        assertThat(savedIntent.createdAt()).isEqualTo(Instant.now(clock));
    }
}
