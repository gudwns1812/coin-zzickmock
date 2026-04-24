package coin.coinzzickmock.feature.order.domain;

public record FuturesOrder(
        String orderId,
        String symbol,
        String positionSide,
        String orderType,
        String marginMode,
        int leverage,
        double quantity,
        Double limitPrice,
        String status,
        String feeType,
        double estimatedFee,
        double executionPrice
) {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FILLED = "FILLED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static FuturesOrder place(
            String orderId,
            String symbol,
            String positionSide,
            String orderType,
            String marginMode,
            int leverage,
            double quantity,
            Double limitPrice,
            boolean executable,
            String feeType,
            double estimatedFee,
            double executionPrice
    ) {
        return new FuturesOrder(
                orderId,
                symbol,
                positionSide,
                orderType,
                marginMode,
                leverage,
                quantity,
                limitPrice,
                executable ? STATUS_FILLED : STATUS_PENDING,
                feeType,
                estimatedFee,
                executionPrice
        );
    }

    public boolean isPending() {
        return STATUS_PENDING.equalsIgnoreCase(status);
    }

    public FuturesOrder fill(double executionPrice, String feeType, double estimatedFee) {
        return new FuturesOrder(
                orderId,
                symbol,
                positionSide,
                orderType,
                marginMode,
                leverage,
                quantity,
                limitPrice,
                STATUS_FILLED,
                feeType,
                estimatedFee,
                executionPrice
        );
    }

    public FuturesOrder cancel() {
        return new FuturesOrder(
                orderId,
                symbol,
                positionSide,
                orderType,
                marginMode,
                leverage,
                quantity,
                limitPrice,
                STATUS_CANCELLED,
                feeType,
                estimatedFee,
                executionPrice
        );
    }
}
