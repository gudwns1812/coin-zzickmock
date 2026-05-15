package coin.coinzzickmock.feature.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.position.domain.PositionCloseOutcome;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import org.junit.jupiter.api.Test;

class LiquidationSettlementTest {
    @Test
    void liquidationFloorsWalletAndAvailableMarginAndReportsCappedLoss() {
        TradingAccount account = new TradingAccount(1L, "demo@example.com", "Demo", 50, 10);
        PositionCloseOutcome rawOutcome = PositionSnapshot.open("BTCUSDT", "LONG", "ISOLATED", 10, 1, 100, 100)
                .close(1, 0, 0, 0);

        LiquidationSettlement settlement = LiquidationSettlement.from(account, rawOutcome.grossRealizedPnl(), rawOutcome.closeFee(), rawOutcome.releasedMargin());
        TradingAccount settledAccount = account.settleLiquidationClose(rawOutcome.grossRealizedPnl(), rawOutcome.closeFee(), rawOutcome.releasedMargin());

        assertThat(settlement.rawNetRealizedPnl()).isEqualTo(-100);
        assertThat(settlement.nextWalletBalance()).isZero();
        assertThat(settlement.walletLossCappedAmount()).isEqualTo(50);
        assertThat(settlement.nextAvailableMargin()).isZero();
        assertThat(settlement.availableLossCappedAmount()).isEqualTo(80);
        assertThat(settlement.lossCappedAmount()).isEqualTo(80);
        assertThat(settlement.settledGrossRealizedPnl()).isEqualTo(-50);
        assertThat(settledAccount.walletBalance()).isZero();
        assertThat(settledAccount.availableMargin()).isZero();
    }

    @Test
    void liquidationKeepsRawCloseOutcomeSeparateFromSettledAccountMutation() {
        TradingAccount account = new TradingAccount(1L, "demo@example.com", "Demo", 50, 10);
        PositionCloseOutcome rawOutcome = PositionSnapshot.open("BTCUSDT", "LONG", "ISOLATED", 10, 1, 100, 100)
                .close(1, 0, 0, 0);

        TradingAccount settledAccount = account.settleLiquidationClose(rawOutcome.grossRealizedPnl(), rawOutcome.closeFee(), rawOutcome.releasedMargin());

        assertThat(rawOutcome.grossRealizedPnl()).isEqualTo(-100);
        assertThat(rawOutcome.netRealizedPnl()).isEqualTo(-100);
        assertThat(settledAccount.walletBalance()).isZero();
        assertThat(settledAccount.availableMargin()).isZero();
    }

    @Test
    void normalCloseSettlementRejectsNegativeBalancesBeforeDatabaseCheck() {
        TradingAccount account = new TradingAccount(1L, "demo@example.com", "Demo", 50, 10);

        var thrown = org.junit.jupiter.api.Assertions.assertThrows(
                coin.coinzzickmock.common.error.CoreException.class,
                () -> account.settlePositionClose(-100, 0, 10)
        );

        assertThat(thrown.errorCode()).isEqualTo(coin.coinzzickmock.common.error.ErrorCode.INSUFFICIENT_AVAILABLE_MARGIN);
    }

}
