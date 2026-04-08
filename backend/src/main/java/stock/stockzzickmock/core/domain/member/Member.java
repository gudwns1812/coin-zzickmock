package stock.stockzzickmock.core.domain.member;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member {

    private final String memberId;
    private final String account;
    private final String passwordHash;
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final String zipCode;
    private final String address;
    private final String addressDetail;
    private Integer invest;
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
