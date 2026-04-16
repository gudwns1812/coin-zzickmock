package coin.coinzzickmock.feature.reward.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.application.result.ShopItemResult;
import coin.coinzzickmock.feature.reward.application.service.GetRewardPointService;
import coin.coinzzickmock.feature.reward.application.service.GetShopItemsService;
import coin.coinzzickmock.providers.Providers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/futures")
public class RewardController {
    private final GetRewardPointService getRewardPointService;
    private final GetShopItemsService getShopItemsService;
    private final Providers providers;

    public RewardController(
            GetRewardPointService getRewardPointService,
            GetShopItemsService getShopItemsService,
            Providers providers
    ) {
        this.getRewardPointService = getRewardPointService;
        this.getShopItemsService = getShopItemsService;
        this.providers = providers;
    }

    @GetMapping("/rewards/me")
    public ApiResponse<RewardPointResponse> me() {
        RewardPointResult result = getRewardPointService.get(providers.auth().currentActor().memberId());
        return ApiResponse.success(new RewardPointResponse(result.rewardPoint(), result.tierLabel()));
    }

    @GetMapping("/shop/items")
    public ApiResponse<List<ShopItemResponse>> shopItems() {
        List<ShopItemResponse> responses = getShopItemsService.getItems().stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    private ShopItemResponse toResponse(ShopItemResult result) {
        return new ShopItemResponse(result.code(), result.name(), result.price(), result.description());
    }
}
