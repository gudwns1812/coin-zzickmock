package coin.coinzzickmock.feature.positionpeek.web;

public record PositionPeekRequest(
        String targetToken,
        String idempotencyKey
) {
}
