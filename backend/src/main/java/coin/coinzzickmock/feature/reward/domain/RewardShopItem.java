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

    public RewardShopItem {
        if (price <= 0) {
            throw invalid("상품 가격은 0보다 커야 합니다.");
        }
        if (soldQuantity < 0) {
            throw invalid("판매 수량은 음수일 수 없습니다.");
        }
        if (totalStock != null && totalStock < 0) {
            throw invalid("총 재고는 음수일 수 없습니다.");
        }
        if (totalStock != null && soldQuantity > totalStock) {
            throw invalid("판매 수량은 총 재고를 초과할 수 없습니다.");
        }
        if (perMemberPurchaseLimit != null && perMemberPurchaseLimit <= 0) {
            throw invalid("회원별 구매 제한은 0보다 커야 합니다.");
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

    public Integer remainingStock() {
        if (!finiteStock()) {
            return null;
        }
        return Math.max(0, totalStock - soldQuantity);
    }

    public RewardShopItem reserveOne() {
        if (!active) {
            throw invalid("비활성 상품은 구매할 수 없습니다.");
        }
        if (soldOut()) {
            throw invalid("품절된 상품입니다.");
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
            throw invalid("판매 수량은 음수로 복구할 수 없습니다.");
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

    private static CoreException invalid(String message) {
        return new CoreException(ErrorCode.INVALID_REQUEST, message);
    }
}
