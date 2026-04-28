package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.result.ShopItemResult;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetShopItemsService {
    private final RewardShopItemRepository rewardShopItemRepository;

    public List<ShopItemResult> getItems() {
        return rewardShopItemRepository.findActiveItems().stream()
                .map(this::toResult)
                .toList();
    }

    private ShopItemResult toResult(RewardShopItem item) {
        return new ShopItemResult(
                item.code(),
                item.name(),
                item.description(),
                item.price(),
                item.active(),
                item.totalStock(),
                item.soldQuantity(),
                item.remainingStock(),
                item.perMemberPurchaseLimit(),
                item.remainingPurchaseLimit(0)
        );
    }
}
