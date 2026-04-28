package coin.coinzzickmock.feature.reward.domain;

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
    public RewardShopItem {
        if (price <= 0) {
            throw new IllegalArgumentException("상품 가격은 0보다 커야 합니다.");
        }
        if (soldQuantity < 0) {
            throw new IllegalArgumentException("판매 수량은 음수일 수 없습니다.");
        }
        if (totalStock != null && totalStock < 0) {
            throw new IllegalArgumentException("총 재고는 음수일 수 없습니다.");
        }
        if (totalStock != null && soldQuantity > totalStock) {
            throw new IllegalArgumentException("판매 수량은 총 재고를 초과할 수 없습니다.");
        }
        if (perMemberPurchaseLimit != null && perMemberPurchaseLimit <= 0) {
            throw new IllegalArgumentException("회원별 구매 제한은 0보다 커야 합니다.");
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

    public Integer remainingStock() {
        if (!finiteStock()) {
            return null;
        }
        return Math.max(0, totalStock - soldQuantity);
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
}
