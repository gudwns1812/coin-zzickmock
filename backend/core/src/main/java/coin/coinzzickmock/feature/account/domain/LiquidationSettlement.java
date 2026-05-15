package coin.coinzzickmock.feature.account.domain;

/**
 * Account-side settlement for forced liquidation.
 *
 * <p>The close outcome keeps the raw mark-price liquidation economics for events and
 * position history. This settlement caps account losses at zero balances so a forced
 * liquidation cannot persist negative wallet or available margin.</p>
 */
public record LiquidationSettlement(
        double rawNetRealizedPnl,
        double rawWalletAfter,
        double nextWalletBalance,
        double walletDelta,
        double walletLossCappedAmount,
        double rawAvailableAfter,
        double nextAvailableMargin,
        double availableMarginDelta,
        double availableLossCappedAmount,
        double lossCappedAmount,
        double settledGrossRealizedPnl
) {
    public static LiquidationSettlement from(
            TradingAccount current,
            double grossRealizedPnl,
            double closeFee,
            double releasedMargin
    ) {
        double rawNet = grossRealizedPnl - closeFee;
        double rawWalletAfter = current.walletBalance() + rawNet;
        double nextWalletBalance = Math.max(0, rawWalletAfter);
        double walletDelta = nextWalletBalance - current.walletBalance();
        double walletLossCappedAmount = Math.max(0, -rawWalletAfter);

        double rawAvailableAfter = current.availableMargin() + releasedMargin + rawNet;
        double nextAvailableMargin = Math.max(0, rawAvailableAfter);
        double availableMarginDelta = nextAvailableMargin - current.availableMargin();
        double availableLossCappedAmount = Math.max(0, -rawAvailableAfter);

        double lossCappedAmount = Math.max(walletLossCappedAmount, availableLossCappedAmount);
        double settledGrossRealizedPnl = walletDelta + closeFee;

        return new LiquidationSettlement(
                rawNet,
                rawWalletAfter,
                nextWalletBalance,
                walletDelta,
                walletLossCappedAmount,
                rawAvailableAfter,
                nextAvailableMargin,
                availableMarginDelta,
                availableLossCappedAmount,
                lossCappedAmount,
                settledGrossRealizedPnl
        );
    }
}
