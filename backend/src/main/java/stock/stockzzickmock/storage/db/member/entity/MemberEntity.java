package stock.stockzzickmock.storage.db.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.domain.member.Member;
import stock.stockzzickmock.storage.db.BaseTimeEntity;

@Entity
@Table(name = "member")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberEntity extends BaseTimeEntity {

    @Id
    @Column(name = "member_id", nullable = false, updatable = false, length = 36)
    private String memberId;

    @Column(nullable = false, unique = true, length = 50)
    private String account;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", nullable = false, length = 255)
    private String addressDetail;

    @Column(nullable = false)
    private Integer invest;

    @Column(name = "refresh_token_version", nullable = false)
    private Long refreshTokenVersion;

    public Member toDomain() {
        return Member.builder()
                .memberId(memberId)
                .account(account)
                .passwordHash(passwordHash)
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .zipCode(zipCode)
                .address(address)
                .addressDetail(addressDetail)
                .invest(invest)
                .refreshTokenVersion(refreshTokenVersion)
                .build();
    }
}
