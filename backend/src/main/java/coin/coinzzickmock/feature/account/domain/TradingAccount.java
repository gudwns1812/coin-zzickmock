package coin.coinzzickmock.feature.account.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record TradingAccount(
        Long memberId,
        String memberEmail,
        String memberName,
        double walletBalance,
        double availableMargin,
        long version
) {
    public static final double INITIAL_WALLET_BALANCE = 100_000d;
    public static final double INITIAL_AVAILABLE_MARGIN = 100_000d;

    public TradingAccount(
            Long memberId,
            String memberEmail,
            String memberName,
            double walletBalance,
            double availableMargin
    ) {
        this(memberId, memberEmail, memberName, walletBalance, availableMargin, 0);
    }

    public static TradingAccount openDefault(Long memberId, String memberEmail, String memberName) {
        return new TradingAccount(
                memberId,
                memberEmail,
                memberName,
                INITIAL_WALLET_BALANCE,
                INITIAL_AVAILABLE_MARGIN,
                0
        );
    }

    public TradingAccount withVersion(long version) {
        return new TradingAccount(memberId, memberEmail, memberName, walletBalance, availableMargin, version);
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
                availableMargin - requiredMargin,
                version
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
                nextAvailableMargin,
                version
        );
    }

    public TradingAccount settlePositionClose(double grossRealizedPnl, double closeFee, double releasedMargin) {
        return new TradingAccount(
                memberId,
                memberEmail,
                memberName,
                walletBalance + grossRealizedPnl - closeFee,
                availableMargin + releasedMargin + grossRealizedPnl - closeFee,
                version
        );
    }

    public boolean refillableToInitialBalance() {
        return walletBalance < INITIAL_WALLET_BALANCE && availableMargin < INITIAL_AVAILABLE_MARGIN;
    }

    public TradingAccount refillToInitialBalance() {
        if (!refillableToInitialBalance()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "이미 리필 기준 잔고에 도달했습니다.");
        }
        return new TradingAccount(
                memberId,
                memberEmail,
                memberName,
                INITIAL_WALLET_BALANCE,
                INITIAL_AVAILABLE_MARGIN,
                version
        );
    }
}
