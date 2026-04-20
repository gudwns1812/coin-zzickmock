package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.result.ShopItemResult;
import coin.coinzzickmock.feature.reward.domain.RewardShopCatalog;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetShopItemsService {
    public List<ShopItemResult> getItems() {
        return RewardShopCatalog.defaultItems().stream()
                .map(this::toResult)
                .toList();
    }

    private ShopItemResult toResult(RewardShopItem item) {
        return new ShopItemResult(item.code(), item.name(), item.price(), item.description());
    }
}
