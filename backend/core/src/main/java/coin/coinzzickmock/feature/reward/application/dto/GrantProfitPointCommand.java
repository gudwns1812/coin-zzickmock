package coin.coinzzickmock.feature.reward.application.dto;

public record GrantProfitPointCommand(
        Long memberId,
        double realizedProfit
) {
}
