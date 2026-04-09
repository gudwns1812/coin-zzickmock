package stock.stockzzickmock.core.domain.member;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member {

    private final String memberId;
    private final MemberAccount account;
    private final MemberProfile profile;
    private final Address address;
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
                .account(MemberAccount.of(account, passwordHash))
                .profile(MemberProfile.of(name, email, phoneNumber))
                .address(Address.of(zipCode, address, addressDetail))
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

    public boolean matchPassword(PasswordEncoder passwordEncoder, @NotBlank String password) {
        return passwordEncoder.matches(password, account.getPasswordHash());
    }
}
