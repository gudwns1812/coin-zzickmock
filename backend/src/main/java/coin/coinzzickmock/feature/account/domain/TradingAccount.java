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
    public static final double INITIAL_WALLET_BALANCE = 100_000d;
    private static final double INITIAL_AVAILABLE_MARGIN = 100_000d;

    public static TradingAccount openDefault(String memberId, String memberEmail, String memberName) {
        return new TradingAccount(
                memberId,
                memberEmail,
                memberName,
                INITIAL_WALLET_BALANCE,
                INITIAL_AVAILABLE_MARGIN
        );
    }

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

    public TradingAccount adjustAvailableMarginForLeverageChange(double oldInitialMargin, double newInitialMargin) {
        double marginDelta = oldInitialMargin - newInitialMargin;
        double nextAvailableMargin = availableMargin + marginDelta;
        if (nextAvailableMargin < 0) {
            throw new CoreException(ErrorCode.INSUFFICIENT_AVAILABLE_MARGIN);
        }
        return new TradingAccount(
                memberId,
                memberEmail,
                memberName,
                walletBalance,
                nextAvailableMargin
        );
    }

    public TradingAccount settlePositionClose(double grossRealizedPnl, double closeFee, double releasedMargin) {
        return new TradingAccount(
                memberId,
                memberEmail,
                memberName,
                walletBalance + grossRealizedPnl - closeFee,
                availableMargin + releasedMargin + grossRealizedPnl - closeFee
        );
    }
}
