package coin.coinzzickmock.feature.reward.application.command;

public record GrantProfitPointCommand(
        Long memberId,
        double realizedProfit
) {
}
