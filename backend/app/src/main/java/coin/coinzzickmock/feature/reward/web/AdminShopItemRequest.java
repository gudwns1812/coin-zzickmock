package coin.coinzzickmock.feature.reward.web;

import coin.coinzzickmock.feature.reward.application.service.AdminRewardShopItemService.AdminShopItemCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record AdminShopItemRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotBlank String itemType,
        @PositiveOrZero int price,
        Boolean active,
        @PositiveOrZero Integer totalStock,
        @PositiveOrZero Integer perMemberPurchaseLimit,
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
