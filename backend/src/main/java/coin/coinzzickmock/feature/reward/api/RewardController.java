package coin.coinzzickmock.feature.reward.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.result.RewardPointHistoryResult;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.application.result.RewardRedemptionResult;
import coin.coinzzickmock.feature.reward.application.result.ShopItemResult;
import coin.coinzzickmock.feature.reward.application.service.AdminRewardRedemptionService;
import coin.coinzzickmock.feature.reward.application.service.CreateRewardRedemptionService;
import coin.coinzzickmock.feature.reward.application.service.GetRewardPointHistoryService;
import coin.coinzzickmock.feature.reward.application.service.GetRewardPointService;
import coin.coinzzickmock.feature.reward.application.service.GetShopItemsService;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.Providers;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/futures")
@RequiredArgsConstructor
public class RewardController {
    private final GetRewardPointService getRewardPointService;
    private final GetRewardPointHistoryService getRewardPointHistoryService;
    private final GetShopItemsService getShopItemsService;
    private final CreateRewardRedemptionService createRewardRedemptionService;
    private final AdminRewardRedemptionService adminRewardRedemptionService;
    private final Providers providers;

    @GetMapping("/rewards/me")
    public ApiResponse<RewardPointResponse> me() {
        RewardPointResult result = getRewardPointService.get(providers.auth().currentActor().memberId());
        return ApiResponse.success(new RewardPointResponse(result.rewardPoint(), result.tierLabel()));
    }

    @GetMapping("/rewards/history")
    public ApiResponse<List<RewardPointHistoryResponse>> history() {
        List<RewardPointHistoryResult> results = getRewardPointHistoryService.get(providers.auth().currentActor().memberId());
        return ApiResponse.success(results.stream()
                .map(RewardPointHistoryResponse::from)
                .toList());
    }

    @GetMapping("/shop/items")
    public ApiResponse<List<ShopItemResponse>> shopItems() {
        List<ShopItemResponse> responses = getShopItemsService.getItems(providers.auth().currentActor().memberId()).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping("/shop/redemptions")
    public ApiResponse<RewardRedemptionResponse> createRedemption(@RequestBody CreateRedemptionRequest request) {
        Actor actor = providers.auth().currentActor();
        RewardRedemptionResult result = createRewardRedemptionService.create(
                actor.memberId(),
                request.itemCode(),
                request.phoneNumber()
        );
        return ApiResponse.success(RewardRedemptionResponse.from(result));
    }

    @GetMapping("/admin/reward-redemptions")
    public ApiResponse<List<RewardRedemptionResponse>> adminRedemptions(
            @RequestParam(defaultValue = "PENDING") RewardRedemptionStatus status
    ) {
        requireAdmin();
        return ApiResponse.success(adminRewardRedemptionService.list(status).stream()
                .map(RewardRedemptionResponse::from)
                .toList());
    }

    @PostMapping("/admin/reward-redemptions/{requestId}/send")
    public ApiResponse<RewardRedemptionResponse> markRedemptionSent(
            @PathVariable String requestId,
            @RequestBody(required = false) AdminRedemptionActionRequest request
    ) {
        Actor actor = requireAdmin();
        RewardRedemptionResult result = adminRewardRedemptionService.markSent(
                requestId,
                actor.memberId(),
                request == null ? null : request.memo()
        );
        return ApiResponse.success(RewardRedemptionResponse.from(result));
    }

    @PostMapping("/admin/reward-redemptions/{requestId}/cancel")
    public ApiResponse<RewardRedemptionResponse> cancelRedemption(
            @PathVariable String requestId,
            @RequestBody(required = false) AdminRedemptionActionRequest request
    ) {
        Actor actor = requireAdmin();
        RewardRedemptionResult result = adminRewardRedemptionService.cancelAndRefund(
                requestId,
                actor.memberId(),
                request == null ? null : request.memo()
        );
        return ApiResponse.success(RewardRedemptionResponse.from(result));
    }

    private Actor requireAdmin() {
        Actor actor = providers.auth().currentActor();
        if (!actor.admin()) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        return actor;
    }

    private ShopItemResponse toResponse(ShopItemResult result) {
        return new ShopItemResponse(
                result.code(),
                result.name(),
                result.description(),
                result.price(),
                result.active(),
                result.totalStock(),
                result.soldQuantity(),
                result.remainingStock(),
                result.perMemberPurchaseLimit(),
                result.remainingPurchaseLimit()
        );
    }
}
