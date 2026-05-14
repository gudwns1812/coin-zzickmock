package coin.coinzzickmock.feature.community.infrastructure;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.gateway.CommunityImageUploadGateway;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class S3CommunityImageUploadGateway implements CommunityImageUploadGateway {
    private final S3Presigner presigner;
    private final String bucket;
    private final String publicBaseUrl;

    public S3CommunityImageUploadGateway(S3Presigner presigner, String bucket, String publicBaseUrl) {
        this.presigner = Objects.requireNonNull(presigner, "presigner");
        this.bucket = requireText(bucket);
        this.publicBaseUrl = stripTrailingSlash(requireText(publicBaseUrl));
    }

    @Override
    public URL generatePresignedPutUrl(String objectKey, String contentType, long contentLength, Duration expiration) {
        if (contentLength <= 0 || expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw invalid();
        }
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(requireObjectKey(objectKey))
                .contentType(requireText(contentType))
                .contentLength(contentLength)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(objectRequest)
                .build();
        return presigner.presignPutObject(presignRequest).url();
    }

    @Override
    public String getPublicUrl(String objectKey) {
        return publicBaseUrl + "/" + requireObjectKey(objectKey);
    }

    private static String requireObjectKey(String objectKey) {
        String value = requireText(objectKey);
        if (value.startsWith("/") || value.contains("..")) {
            throw invalid();
        }
        return value;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw invalid();
        }
        return value.trim();
    }

    private static String stripTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
