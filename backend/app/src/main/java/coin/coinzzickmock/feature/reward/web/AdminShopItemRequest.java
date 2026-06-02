package coin.coinzzickmock.feature.reward.web;

import coin.coinzzickmock.feature.reward.application.service.AdminRewardShopItemService.AdminShopItemCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.Default;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record AdminShopItemRequest(
        @NotBlank(groups = Create.class) String code,
        @NotBlank String name,
        @NotBlank String description,
        @NotBlank String itemType,
        @Positive int price,
        Boolean active,
        @PositiveOrZero Integer totalStock,
        @Positive Integer perMemberPurchaseLimit,
        int sortOrder
) {
    public interface Create extends Default {
    }

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
