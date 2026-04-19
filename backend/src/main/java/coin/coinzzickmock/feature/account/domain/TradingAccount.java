package coin.coinzzickmock.feature.account.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record TradingAccount(
        String memberId,
        String memberEmail,
        String memberName,
        double walletBalance,
        double availableMargin
) {
    public TradingAccount reserveForFilledOrder(double estimatedFee, double estimatedInitialMargin) {
        double requiredMargin = estimatedInitialMargin + estimatedFee;
        if (availableMargin < requiredMargin) {
            throw new CoreException(ErrorCode.INSUFFICIENT_AVAILABLE_MARGIN);
        }
        return new TradingAccount(
                memberId,
                memberEmail,
                memberName,
                walletBalance - estimatedFee,
                availableMargin - requiredMargin
        );
    }

    public TradingAccount settlePositionClose(double realizedPnl, double closeFee, double releasedMargin) {
        return new TradingAccount(
                memberId,
                memberEmail,
                memberName,
                walletBalance + realizedPnl - closeFee,
                availableMargin + releasedMargin + realizedPnl - closeFee
        );
    }
}
