package coin.coinzzickmock.storage.db.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import coin.coinzzickmock.core.domain.member.Member;
import coin.coinzzickmock.core.domain.member.MemberAccount;
import coin.coinzzickmock.core.domain.member.MemberProfile;
import coin.coinzzickmock.storage.db.Address;
import coin.coinzzickmock.storage.db.BaseTimeEntity;
import coin.coinzzickmock.support.error.AuthErrorType;
import coin.coinzzickmock.support.error.CoreException;

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

    @Embedded
    private Address address;

    @Column(nullable = false)
    private Integer invest;

    @Column(name = "refresh_token_version", nullable = false)
    private Long refreshTokenVersion;

    public Member toDomain() {
        return Member.builder()
                .memberId(memberId)
                .account(MemberAccount.of(account, passwordHash))
                .profile(MemberProfile.of(name, email, phoneNumber))
                .address(coin.coinzzickmock.core.domain.member.Address.of(
                        address.getZipCode(),
                        address.getAddress(),
                        address.getAddressDetail()))
                .invest(invest)
                .refreshTokenVersion(refreshTokenVersion)
                .build();
    }

    public void updateInvest(int investScore) {
        this.invest = investScore;
    }

    public void updateRefreshVersion(Long version) {
        if (this.refreshTokenVersion != version) {
            throw new CoreException(AuthErrorType.INVALID_JWT);
        }

        this.refreshTokenVersion += 1;
    }
}
