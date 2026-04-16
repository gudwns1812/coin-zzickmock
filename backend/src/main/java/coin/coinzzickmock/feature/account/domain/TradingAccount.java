package coin.coinzzickmock.feature.account.domain;

public record TradingAccount(
        String memberId,
        String memberEmail,
        String memberName,
        double walletBalance,
        double availableMargin
) {
}
