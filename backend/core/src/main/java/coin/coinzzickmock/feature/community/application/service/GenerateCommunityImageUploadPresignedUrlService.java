package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.feature.community.application.CommunityImageUploadSettings;
import coin.coinzzickmock.feature.community.application.command.GenerateCommunityImageUploadPresignedUrlCommand;
import coin.coinzzickmock.feature.community.application.gateway.CommunityImageUploadGateway;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostImageRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityImageUploadPresignedUrlResult;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageIntent;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageStatus;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GenerateCommunityImageUploadPresignedUrlService {
    private final CommunityImageUploadGateway uploadGateway;
    private final CommunityPostImageRepository imageRepository;
    private final CommunityImageUploadSettings uploadSettings;
    private final Clock clock;

    @Transactional
    public CommunityImageUploadPresignedUrlResult execute(GenerateCommunityImageUploadPresignedUrlCommand command) {
        String contentType = uploadSettings.normalizeAllowedContentType(command.contentType());
        uploadSettings.validate(contentType, command.contentLength());
        String objectKey = uploadSettings.objectKeyPrefix() + "/" + command.actorMemberId() + "/"
                + UUID.randomUUID() + uploadSettings.extensionFor(contentType);
        URL presignedUrl = uploadGateway.generatePresignedPutUrl(
                objectKey,
                contentType,
                command.contentLength(),
                uploadSettings.presignTtl()
        );
        String publicUrl = uploadGateway.getPublicUrl(objectKey);

        Instant now = Instant.now(clock);
        CommunityPostImageIntent intent = new CommunityPostImageIntent(
                null,
                null,
                command.actorMemberId(),
                objectKey,
                publicUrl,
                contentType,
                command.contentLength(),
                CommunityPostImageStatus.PRESIGNED,
                now,
                now
        );

        imageRepository.saveImageIntent(intent);

        return new CommunityImageUploadPresignedUrlResult(
                objectKey,
                presignedUrl.toString(),
                publicUrl,
                contentType,
                now.plus(uploadSettings.presignTtl()),
                uploadSettings.maxBytes()
        );
    }
}
