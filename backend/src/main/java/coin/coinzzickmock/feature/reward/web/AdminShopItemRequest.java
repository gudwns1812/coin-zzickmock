package coin.coinzzickmock.feature.reward.web;

import coin.coinzzickmock.feature.reward.application.service.AdminRewardShopItemService.AdminShopItemCommand;

public record AdminShopItemRequest(
        String code,
        String name,
        String description,
        String itemType,
        int price,
        Boolean active,
        Integer totalStock,
        Integer perMemberPurchaseLimit,
        int sortOrder
) {
    public AdminShopItemCommand toCommand() {
        return new AdminShopItemCommand(
                code,
                name,
                description,
                itemType,
                price,
                active == null || active,
                totalStock,
                perMemberPurchaseLimit,
                sortOrder
        );
    }
}
