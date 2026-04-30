package coin.coinzzickmock.feature.account.domain;

import java.time.Instant;

public record WalletHistorySource(
        String sourceType,
        String sourceReference
) {
    public static final String TYPE_SEED = "SEED";
    public static final String TYPE_ORDER_RESERVE = "ORDER_RESERVE";
    public static final String TYPE_ORDER_CANCEL_RELEASE = "ORDER_CANCEL_RELEASE";
    public static final String TYPE_ORDER_FILL = "ORDER_FILL";
    public static final String TYPE_PARTIAL_FILL = "PARTIAL_FILL";
    public static final String TYPE_POSITION_CLOSE = "POSITION_CLOSE";
    public static final String TYPE_POSITION_LEVERAGE_CHANGE = "POSITION_LEVERAGE_CHANGE";
    public static final String TYPE_LIQUIDATION = "LIQUIDATION";
    public static final String TYPE_MANUAL_ADJUSTMENT = "MANUAL_ADJUSTMENT";

    public WalletHistorySource {
        if (sourceType == null || sourceType.isBlank()) {
            throw new IllegalArgumentException("sourceType is required");
        }
        if (sourceReference == null || sourceReference.isBlank()) {
            throw new IllegalArgumentException("sourceReference is required");
        }
        if (sourceReference.length() > 255) {
            throw new IllegalArgumentException("sourceReference must be 255 characters or less");
        }
    }

    public static WalletHistorySource seed(Long memberId) {
        return new WalletHistorySource(TYPE_SEED, "account:" + memberId + ":seed");
    }

    public static WalletHistorySource orderReserve(String orderId) {
        return new WalletHistorySource(TYPE_ORDER_RESERVE, "order:" + orderId + ":reserve");
    }

    public static WalletHistorySource orderCancelRelease(String orderId) {
        return new WalletHistorySource(TYPE_ORDER_CANCEL_RELEASE, "order:" + orderId + ":cancel-release");
    }

    public static WalletHistorySource orderFill(String orderId) {
        return new WalletHistorySource(TYPE_ORDER_FILL, "order:" + orderId + ":fill");
    }

    public static WalletHistorySource partialFill(String orderId, String fillId) {
        return new WalletHistorySource(TYPE_PARTIAL_FILL, "order:" + orderId + ":partial-fill:" + fillId);
    }

    public static WalletHistorySource positionCloseOrderFill(String orderId) {
        return new WalletHistorySource(TYPE_POSITION_CLOSE, "order:" + orderId + ":close-fill");
    }

    public static WalletHistorySource positionClose(String closeReason, String symbol, String positionSide,
                                                    String marginMode, Instant eventTime) {
        return new WalletHistorySource(
                TYPE_POSITION_CLOSE,
                "position:" + symbol + ":" + positionSide + ":" + marginMode + ":" + closeReason + ":"
                        + eventTime.toEpochMilli()
        );
    }

    public static WalletHistorySource positionLeverageChange(String symbol, String positionSide, String marginMode,
                                                             Instant eventTime) {
        return new WalletHistorySource(
                TYPE_POSITION_LEVERAGE_CHANGE,
                "position:" + symbol + ":" + positionSide + ":" + marginMode + ":leverage:"
                        + eventTime.toEpochMilli()
        );
    }

    public static WalletHistorySource liquidation(String symbol, String positionSide, String marginMode,
                                                  Instant eventTime) {
        return new WalletHistorySource(
                TYPE_LIQUIDATION,
                "liquidation:" + symbol + ":" + positionSide + ":" + marginMode + ":" + eventTime.toEpochMilli()
        );
    }

    public static WalletHistorySource manualAdjustment(String reference) {
        return new WalletHistorySource(TYPE_MANUAL_ADJUSTMENT, "manual:" + reference);
    }
}
