package coin.coinzzickmock.feature.reward.web;

public record CreateRedemptionRequest(
        String itemCode,
        String phoneNumber
) {
}
