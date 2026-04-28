package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_credentials")
public class MemberCredentialEntity extends AuditableEntity {
    @Id
    @Column(name = "member_id", nullable = false, length = 64)
    private String memberId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "member_name", nullable = false, length = 100)
    private String memberName;

    @Column(name = "member_email", nullable = false, length = 255)
    private String memberEmail;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", nullable = false, length = 255)
    private String addressDetail;

    @Column(name = "invest_score", nullable = false)
    private int investScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    protected MemberCredentialEntity() {
    }

    public MemberCredentialEntity(
            String memberId,
            String passwordHash,
            String memberName,
            String memberEmail,
            String phoneNumber,
            String zipCode,
            String address,
            String addressDetail,
            int investScore,
            MemberRole role
    ) {
        this.memberId = memberId;
        this.passwordHash = passwordHash;
        this.memberName = memberName;
        this.memberEmail = memberEmail;
        this.phoneNumber = phoneNumber;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.investScore = investScore;
        this.role = role == null ? MemberRole.USER : role;
    }

    public static MemberCredentialEntity from(MemberCredential memberCredential) {
        return new MemberCredentialEntity(
                memberCredential.memberId(),
                memberCredential.passwordHash(),
                memberCredential.memberName(),
                memberCredential.memberEmail(),
                memberCredential.phoneNumber(),
                memberCredential.zipCode(),
                memberCredential.address(),
                memberCredential.addressDetail(),
                memberCredential.investScore(),
                memberCredential.role()
        );
    }

    public void apply(MemberCredential memberCredential) {
        this.passwordHash = memberCredential.passwordHash();
        this.memberName = memberCredential.memberName();
        this.memberEmail = memberCredential.memberEmail();
        this.phoneNumber = memberCredential.phoneNumber();
        this.zipCode = memberCredential.zipCode();
        this.address = memberCredential.address();
        this.addressDetail = memberCredential.addressDetail();
        this.investScore = memberCredential.investScore();
        this.role = memberCredential.role() == null ? MemberRole.USER : memberCredential.role();
    }

    public MemberCredential toDomain() {
        return new MemberCredential(
                memberId,
                passwordHash,
                memberName,
                memberEmail,
                phoneNumber,
                zipCode,
                address,
                addressDetail,
                investScore,
                role
        );
    }
}
