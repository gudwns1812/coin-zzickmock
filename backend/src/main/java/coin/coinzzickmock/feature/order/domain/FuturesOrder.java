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
                executable ? "FILLED" : "PENDING",
                feeType,
                estimatedFee,
                executionPrice
        );
    }
}
