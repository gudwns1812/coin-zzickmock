package stock.stockzzickmock.core.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.storage.db.BaseTimeEntity;

import java.util.UUID;

@Entity
@Table(name = "member")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member extends BaseTimeEntity {

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

    public static Member create(
            String account,
            String passwordHash,
            String name,
            String email,
            String phoneNumber,
            String zipCode,
            String address,
            String addressDetail
    ) {
        return Member.builder()
                .memberId(UUID.randomUUID().toString())
                .account(account)
                .passwordHash(passwordHash)
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .zipCode(zipCode)
                .address(address)
                .addressDetail(addressDetail)
                .invest(0)
                .refreshTokenVersion(0L)
                .build();
    }

    public void updateInvest(int investScore) {
        this.invest = investScore;
    }

    public void updateRefreshTokenVersion() {
        this.refreshTokenVersion += 1;
    }
}
