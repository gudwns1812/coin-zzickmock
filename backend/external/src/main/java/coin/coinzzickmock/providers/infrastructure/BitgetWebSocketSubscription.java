package coin.coinzzickmock.providers.infrastructure;

public record BitgetWebSocketSubscription(
        String instType,
        BitgetWebSocketChannel channel,
        String instId
) {
    public static BitgetWebSocketSubscription usdtFutures(BitgetWebSocketChannel channel, String instId) {
        return new BitgetWebSocketSubscription("USDT-FUTURES", channel, instId);
    }

    public String subscribeMessage() {
        return """
                {"op":"subscribe","args":[{"instType":"%s","channel":"%s","instId":"%s"}]}
                """.formatted(instType, channel.value(), instId).trim();
    }
}
