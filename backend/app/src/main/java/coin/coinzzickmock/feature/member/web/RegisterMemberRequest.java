package coin.coinzzickmock.feature.member.web;

import jakarta.validation.constraints.NotBlank;

public record RegisterMemberRequest(
        @NotBlank String account,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String nickname,
        @NotBlank String phoneNumber,
        @NotBlank String email,
        String fgOffset
) {
}
