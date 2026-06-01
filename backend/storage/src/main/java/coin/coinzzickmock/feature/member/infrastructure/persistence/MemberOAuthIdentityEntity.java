package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.member.domain.MemberOAuthIdentity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "member_oauth_identities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_oauth_identity_provider_subject", columnNames = {"provider", "provider_subject"}),
                @UniqueConstraint(name = "uk_member_oauth_identity_member_provider", columnNames = {"member_id", "provider"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberOAuthIdentityEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "provider_name", length = 255)
    private String providerName;

    public MemberOAuthIdentityEntity(
            Long id,
            Long memberId,
            String provider,
            String providerSubject,
            String providerEmail,
            String providerName
    ) {
        this.id = id;
        this.memberId = memberId;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.providerEmail = providerEmail;
        this.providerName = providerName;
    }

    public static MemberOAuthIdentityEntity from(MemberOAuthIdentity identity) {
        return new MemberOAuthIdentityEntity(
                identity.id(),
                identity.memberId(),
                identity.provider(),
                identity.providerSubject(),
                identity.providerEmail(),
                identity.providerName()
        );
    }

    public MemberOAuthIdentity toDomain() {
        return new MemberOAuthIdentity(
                id,
                memberId,
                provider,
                providerSubject,
                providerEmail,
                providerName,
                createdAt(),
                updatedAt()
        );
    }
}
