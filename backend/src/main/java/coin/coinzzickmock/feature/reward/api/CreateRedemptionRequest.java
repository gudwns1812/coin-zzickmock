package coin.coinzzickmock.feature.reward.api;

public record CreateRedemptionRequest(
        String itemCode,
        String phoneNumber
) {
}
