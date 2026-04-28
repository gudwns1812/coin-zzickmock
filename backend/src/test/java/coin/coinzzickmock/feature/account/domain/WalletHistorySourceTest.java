package coin.coinzzickmock.feature.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WalletHistorySourceTest {
    @Test
    void distinguishesReserveCancelReleaseFillAndPartialFillReferences() {
        WalletHistorySource reserve = WalletHistorySource.orderReserve("order-1");
        WalletHistorySource cancelRelease = WalletHistorySource.orderCancelRelease("order-1");
        WalletHistorySource fill = WalletHistorySource.orderFill("order-1");
        WalletHistorySource partialFill = WalletHistorySource.partialFill("order-1", "fill-1");

        assertThat(reserve.sourceReference()).isEqualTo("order:order-1:reserve");
        assertThat(cancelRelease.sourceReference()).isEqualTo("order:order-1:cancel-release");
        assertThat(fill.sourceReference()).isEqualTo("order:order-1:fill");
        assertThat(partialFill.sourceReference()).isEqualTo("order:order-1:partial-fill:fill-1");
        assertThat(reserve.sourceReference())
                .isNotEqualTo(cancelRelease.sourceReference())
                .isNotEqualTo(fill.sourceReference())
                .isNotEqualTo(partialFill.sourceReference());
    }
}
