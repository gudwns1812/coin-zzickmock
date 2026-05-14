package coin.coinzzickmock.feature.reward.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Instant;

public record RewardShopPurchase(
        String purchaseId,
        Long memberId,
        Long shopItemId,
        String itemCode,
        String itemName,
        String itemType,
        int itemPrice,
        int pointAmount,
        int quantity,
        Instant purchasedAt
) {
    public static RewardShopPurchase instant(
            String purchaseId,
            Long memberId,
            RewardShopItem item,
            Instant purchasedAt
    ) {
        return new RewardShopPurchase(
                purchaseId,
                memberId,
                item.id(),
                item.code(),
                item.name(),
                item.itemType(),
                item.price(),
                item.price(),
                1,
                purchasedAt
        );
    }

    public RewardShopPurchase {
        if (purchaseId == null || purchaseId.isBlank()) {
            throw invalid();
        }
        if (memberId == null || shopItemId == null) {
            throw invalid();
        }
        if (itemCode == null || itemCode.isBlank()) {
            throw invalid();
        }
        if (itemName == null || itemName.isBlank()) {
            throw invalid();
        }
        if (!RewardShopItem.ITEM_TYPE_ACCOUNT_REFILL_COUNT.equals(itemType)
                && !RewardShopItem.ITEM_TYPE_POSITION_PEEK.equals(itemType)) {
            throw invalid();
        }
        if (itemPrice <= 0 || pointAmount <= 0) {
            throw invalid();
        }
        if (quantity != 1) {
            throw invalid();
        }
        if (purchasedAt == null) {
            throw invalid();
        }
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
