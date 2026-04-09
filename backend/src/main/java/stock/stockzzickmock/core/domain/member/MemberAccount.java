package stock.stockzzickmock.core.domain.member;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberAccount {
    private final String account;
    private final String passwordHash;

    public static MemberAccount of(String account, String passwordHash) {
        return MemberAccount.builder()
                .account(account)
                .passwordHash(passwordHash)
                .build();
    }
}
