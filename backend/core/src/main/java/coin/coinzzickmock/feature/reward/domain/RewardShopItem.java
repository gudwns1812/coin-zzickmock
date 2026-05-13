package coin.coinzzickmock.feature.reward.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record RewardShopItem(
        Long id,
        String code,
        String name,
        String description,
        String itemType,
        int price,
        boolean active,
        Integer totalStock,
        int soldQuantity,
        Integer perMemberPurchaseLimit,
        int sortOrder
) {
    public static final String ITEM_TYPE_COFFEE_VOUCHER = "COFFEE_VOUCHER";
    public static final String ITEM_TYPE_ACCOUNT_REFILL_COUNT = "ACCOUNT_REFILL_COUNT";
    public static final String ITEM_TYPE_POSITION_PEEK = "POSITION_PEEK";

    public RewardShopItem {
        if (price <= 0) {
            throw invalid();
        }
        if (soldQuantity < 0) {
            throw invalid();
        }
        if (totalStock != null && totalStock < 0) {
            throw invalid();
        }
        if (totalStock != null && soldQuantity > totalStock) {
            throw invalid();
        }
        if (perMemberPurchaseLimit != null && perMemberPurchaseLimit <= 0) {
            throw invalid();
        }
    }

    public RewardShopItem(
            String code,
            String name,
            int price,
            String description
    ) {
        this(null, code, name, description, "GENERAL", price, true, null, 0, null, 0);
    }

    public boolean finiteStock() {
        return totalStock != null;
    }

    public boolean soldOut() {
        return finiteStock() && soldQuantity >= totalStock;
    }

    public boolean coffeeVoucher() {
        return ITEM_TYPE_COFFEE_VOUCHER.equals(itemType);
    }

    public boolean accountRefillCount() {
        return ITEM_TYPE_ACCOUNT_REFILL_COUNT.equals(itemType);
    }

    public boolean positionPeek() {
        return ITEM_TYPE_POSITION_PEEK.equals(itemType);
    }

    public boolean instantConsumable() {
        return accountRefillCount() || positionPeek();
    }

    public Integer remainingStock() {
        if (!finiteStock()) {
            return null;
        }
        return Math.max(0, totalStock - soldQuantity);
    }

    public RewardShopItem reserveOne() {
        if (!active) {
            throw invalid();
        }
        if (soldOut()) {
            throw invalid();
        }
        return new RewardShopItem(
                id,
                code,
                name,
                description,
                itemType,
                price,
                active,
                totalStock,
                soldQuantity + 1,
                perMemberPurchaseLimit,
                sortOrder
        );
    }

    public RewardShopItem releaseOne() {
        if (soldQuantity == 0) {
            throw invalid();
        }
        return new RewardShopItem(
                id,
                code,
                name,
                description,
                itemType,
                price,
                active,
                totalStock,
                soldQuantity - 1,
                perMemberPurchaseLimit,
                sortOrder
        );
    }

    public boolean hasPurchaseLimit() {
        return perMemberPurchaseLimit != null;
    }

    public boolean memberReachedLimit(int purchaseCount) {
        return hasPurchaseLimit() && purchaseCount >= perMemberPurchaseLimit;
    }

    public Integer remainingPurchaseLimit(int purchaseCount) {
        if (!hasPurchaseLimit()) {
            return null;
        }
        return Math.max(0, perMemberPurchaseLimit - purchaseCount);
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
