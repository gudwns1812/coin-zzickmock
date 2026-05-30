package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.member.domain.MemberOAuthPendingLink;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_oauth_pending_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberOAuthPendingLinkEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 128, unique = true)
    private String tokenHash;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "provider_name", length = 255)
    private String providerName;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_failed_at")
    private Instant lastFailedAt;

    public MemberOAuthPendingLinkEntity(
            Long id,
            String tokenHash,
            String provider,
            String providerSubject,
            String providerEmail,
            String providerName,
            Instant expiresAt,
            Instant consumedAt,
            int attemptCount,
            Instant lastFailedAt
    ) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.providerEmail = providerEmail;
        this.providerName = providerName;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.attemptCount = attemptCount;
        this.lastFailedAt = lastFailedAt;
    }

    public static MemberOAuthPendingLinkEntity from(MemberOAuthPendingLink pendingLink) {
        return new MemberOAuthPendingLinkEntity(
                pendingLink.id(),
                pendingLink.tokenHash(),
                pendingLink.provider(),
                pendingLink.providerSubject(),
                pendingLink.providerEmail(),
                pendingLink.providerName(),
                pendingLink.expiresAt(),
                pendingLink.consumedAt(),
                pendingLink.attemptCount(),
                pendingLink.lastFailedAt()
        );
    }

    public void apply(MemberOAuthPendingLink pendingLink) {
        this.tokenHash = pendingLink.tokenHash();
        this.provider = pendingLink.provider();
        this.providerSubject = pendingLink.providerSubject();
        this.providerEmail = pendingLink.providerEmail();
        this.providerName = pendingLink.providerName();
        this.expiresAt = pendingLink.expiresAt();
        this.consumedAt = pendingLink.consumedAt();
        this.attemptCount = pendingLink.attemptCount();
        this.lastFailedAt = pendingLink.lastFailedAt();
    }

    public MemberOAuthPendingLink toDomain() {
        return new MemberOAuthPendingLink(
                id,
                tokenHash,
                provider,
                providerSubject,
                providerEmail,
                providerName,
                expiresAt,
                consumedAt,
                attemptCount,
                lastFailedAt,
                createdAt(),
                updatedAt()
        );
    }
}
