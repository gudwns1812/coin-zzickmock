package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Instant;

public record CommunityPostImageIntent(
        Long id,
        Long postId,
        Long uploaderMemberId,
        String objectKey,
        String publicUrl,
        String contentType,
        long sizeBytes,
        CommunityPostImageStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public CommunityPostImageIntent {
        if (uploaderMemberId == null || uploaderMemberId <= 0 || objectKey == null || objectKey.isBlank()
                || publicUrl == null || publicUrl.isBlank() || contentType == null || contentType.isBlank()
                || sizeBytes <= 0 || status == null || createdAt == null || updatedAt == null) {
            throw invalid();
        }
        objectKey = objectKey.trim();
        publicUrl = publicUrl.trim();
        contentType = contentType.trim();
    }

    public boolean attachableBy(Long memberId) {
        return uploaderMemberId.equals(memberId)
                && objectKey.startsWith("community/" + memberId + "/")
                && (status == CommunityPostImageStatus.PRESIGNED || status == CommunityPostImageStatus.ATTACHED);
    }

    public CommunityPostImageIntent attachTo(Long postId, Instant updatedAt) {
        if (postId == null || postId <= 0) {
            throw invalid();
        }
        if (status != CommunityPostImageStatus.PRESIGNED && status != CommunityPostImageStatus.ATTACHED) {
            throw invalid();
        }
        return new CommunityPostImageIntent(id, postId, uploaderMemberId, objectKey, publicUrl, contentType,
                sizeBytes, CommunityPostImageStatus.ATTACHED, createdAt, requireTime(updatedAt));
    }

    public CommunityPostImageIntent markOrphaned(Instant updatedAt) {
        return new CommunityPostImageIntent(id, postId, uploaderMemberId, objectKey, publicUrl, contentType,
                sizeBytes, CommunityPostImageStatus.ORPHANED, createdAt, requireTime(updatedAt));
    }

    private static Instant requireTime(Instant value) {
        if (value == null) {
            throw invalid();
        }
        return value;
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
