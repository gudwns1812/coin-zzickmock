package coin.coinzzickmock.feature.account.application.event;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import java.time.Instant;

public record TradingAccountOpenedEvent(
        Long memberId,
        String memberEmail,
        String memberName,
        String nickname,
        double walletBalance,
        double availableMargin,
        long accountVersion,
        Instant openedAt
) {
    public static TradingAccountOpenedEvent from(TradingAccount account, String nickname) {
        return new TradingAccountOpenedEvent(
                account.memberId(),
                account.memberEmail(),
                account.memberName(),
                nickname,
                account.walletBalance(),
                account.availableMargin(),
                account.version(),
                Instant.now()
        );
    }
}
