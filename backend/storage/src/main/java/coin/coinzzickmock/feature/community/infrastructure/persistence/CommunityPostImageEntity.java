package coin.coinzzickmock.feature.community.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
        this.uploaderMemberId = uploaderMemberId;
        this.objectKey = objectKey;
        this.publicUrl = publicUrl;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = status;
    }

    public void attachToPost(Long postId) {
        this.postId = postId;
        this.status = "ATTACHED";
    }

    public void markOrphaned() {
        this.postId = null;
        this.status = "ORPHANED";
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
}
