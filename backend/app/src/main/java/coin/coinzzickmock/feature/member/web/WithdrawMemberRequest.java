package coin.coinzzickmock.feature.member.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WithdrawMemberRequest(
        @NotNull @Positive Long memberId
) {
}
