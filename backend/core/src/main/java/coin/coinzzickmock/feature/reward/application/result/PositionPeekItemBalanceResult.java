package coin.coinzzickmock.feature.reward.application.result;

import coin.coinzzickmock.feature.reward.domain.RewardItemBalance;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import java.util.Optional;

public record PositionPeekItemBalanceResult(
        String itemCode,
        int remainingQuantity
) {
    public static PositionPeekItemBalanceResult from(
            RewardShopItem item,
            Optional<RewardItemBalance> balance
    ) {
        return new PositionPeekItemBalanceResult(
                item.code(),
                balance.map(RewardItemBalance::remainingQuantity).orElse(0)
        );
    }
}
