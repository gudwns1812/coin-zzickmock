package coin.coinzzickmock.feature.positionpeek.application.query;

import coin.coinzzickmock.feature.reward.application.repository.RewardItemBalanceRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PositionPeekItemBalanceReader {
    public static final String POSITION_PEEK_ITEM_CODE = "position.peek";

    private final RewardShopItemRepository rewardShopItemRepository;
    private final RewardItemBalanceRepository rewardItemBalanceRepository;

    @Transactional(readOnly = true)
    public int getRemainingCount(Long memberId) {
        return rewardShopItemRepository.findByCode(POSITION_PEEK_ITEM_CODE)
                .filter(RewardShopItem::positionPeek)
                .flatMap(item -> rewardItemBalanceRepository.findByMemberIdAndShopItemId(memberId, item.id()))
                .map(balance -> Math.max(0, balance.remainingQuantity()))
                .orElse(0);
    }
}
