package stock.stockzzickmock.core.domain.member;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberProfile {
    private final String name;
    private final String email;
    private final String phoneNumber;

    public static MemberProfile of(String name, String email, String phoneNumber) {
        return MemberProfile.builder()
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .build();
    }
}
