package coin.coinzzickmock.feature.community.web.request;

public record CommunityImageUploadPresignRequest(String fileName, String contentType, long sizeBytes) {
}
