package coin.coinzzickmock.feature.community.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageIntent;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "community_post_images")
public class CommunityPostImageEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "uploader_member_id", nullable = false)
    private Long uploaderMemberId;

    @Column(name = "object_key", nullable = false, unique = true, length = 255)
    private String objectKey;

    @Column(name = "public_url", nullable = false, length = 500)
    private String publicUrl;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected CommunityPostImageEntity() {
    }

    public CommunityPostImageEntity(
            Long id,
            Long postId,
            Long uploaderMemberId,
            String objectKey,
            String publicUrl,
            String contentType,
            long sizeBytes,
            String status
    ) {
        this.id = id;
        this.postId = postId;
        this.uploaderMemberId = Objects.requireNonNull(uploaderMemberId, "uploaderMemberId");
        this.objectKey = requireText(objectKey, "objectKey");
        this.publicUrl = requireText(publicUrl, "publicUrl");
        this.contentType = requireText(contentType, "contentType");
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
        this.sizeBytes = sizeBytes;
        this.status = requireStatus(status).name();
    }

    public void attachToPost(Long postId, Instant attachedAt) {
        this.postId = Objects.requireNonNull(postId, "postId");
        Objects.requireNonNull(attachedAt, "attachedAt");
        this.status = CommunityPostImageStatus.ATTACHED.name();
    }

    public void markOrphaned() {
        this.postId = null;
        this.status = CommunityPostImageStatus.ORPHANED.name();
    }


    public CommunityPostImageIntent toDomain() {
        return new CommunityPostImageIntent(id, postId, uploaderMemberId, objectKey, publicUrl, contentType, sizeBytes,
                CommunityPostImageStatus.valueOf(status), createdAt(), updatedAt());
    }

    public Long id() {
        return id;
    }

    public Long postId() {
        return postId;
    }

    public Long uploaderMemberId() {
        return uploaderMemberId;
    }

    public String objectKey() {
        return objectKey;
    }

    public String publicUrl() {
        return publicUrl;
    }

    public String contentType() {
        return contentType;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public String status() {
        return status;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static CommunityPostImageStatus requireStatus(String status) {
        return CommunityPostImageStatus.valueOf(Objects.requireNonNull(status, "status"));
    }
}
