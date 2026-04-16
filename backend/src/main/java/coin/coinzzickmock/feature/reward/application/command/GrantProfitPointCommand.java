package coin.coinzzickmock.feature.reward.application.command;

public record GrantProfitPointCommand(
        String memberId,
        double realizedProfit
) {
}
