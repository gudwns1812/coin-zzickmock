package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopMemberItemUsageRepository;
import coin.coinzzickmock.feature.reward.application.result.ShopItemResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetShopItemsService {
    private final RewardShopItemRepository rewardShopItemRepository;
    private final RewardShopMemberItemUsageRepository rewardShopMemberItemUsageRepository;

    public List<ShopItemResult> getItems(Long memberId) {
        return rewardShopItemRepository.findActiveItems().stream()
                .map(item -> {
                    int purchaseCount = rewardShopMemberItemUsageRepository
                            .findByMemberIdAndShopItemId(memberId, item.id())
                            .map(usage -> usage.purchaseCount())
                            .orElse(0);
                    return ShopItemResult.from(item, purchaseCount);
                })
                .toList();
    }
}
