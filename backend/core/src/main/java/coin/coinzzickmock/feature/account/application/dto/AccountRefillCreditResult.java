package coin.coinzzickmock.feature.account.application.dto;

import coin.coinzzickmock.feature.account.domain.AccountRefillState;

public record AccountRefillCreditResult(
        int remainingCount
) {
    public static AccountRefillCreditResult from(AccountRefillState state) {
        return new AccountRefillCreditResult(state.remainingCount());
    }
}
