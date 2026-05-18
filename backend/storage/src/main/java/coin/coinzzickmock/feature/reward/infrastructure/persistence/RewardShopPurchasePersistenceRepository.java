package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.application.repository.RewardShopPurchaseRepository;
import coin.coinzzickmock.feature.reward.domain.RewardShopPurchase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class RewardShopPurchasePersistenceRepository implements RewardShopPurchaseRepository {
    private final RewardShopPurchaseEntityRepository purchaseEntityRepository;
    private final RewardShopItemEntityRepository shopItemEntityRepository;

    @Override
    @Transactional
    public RewardShopPurchase create(RewardShopPurchase purchase) {
        RewardShopItemEntity shopItem = shopItemEntityRepository.findById(purchase.shopItemId()).orElseThrow();
        RewardShopPurchaseEntity entity = RewardShopPurchaseEntity.from(purchase, shopItem);
        return purchaseEntityRepository.save(entity).toDomain();
    }
}
