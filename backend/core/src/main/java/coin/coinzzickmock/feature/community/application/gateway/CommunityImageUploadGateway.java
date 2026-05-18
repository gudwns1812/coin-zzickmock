package coin.coinzzickmock.feature.community.application.gateway;

import java.net.URL;
import java.time.Duration;

public interface CommunityImageUploadGateway {
    /**
     * Generates a presigned URL for uploading an image using HTTP PUT.
     *
     * @param objectKey     The destination object key in S3.
     * @param contentType   The MIME type of the image.
     * @param contentLength The expected size of the image in bytes.
     * @param expiration    How long the presigned URL should remain valid.
     * @return The presigned URL.
     */
    URL generatePresignedPutUrl(String objectKey, String contentType, long contentLength, Duration expiration);

    /**
     * Returns the public URL for a given object key.
     *
     * @param objectKey The object key.
     * @return The public URL.
     */
    String getPublicUrl(String objectKey);
}
