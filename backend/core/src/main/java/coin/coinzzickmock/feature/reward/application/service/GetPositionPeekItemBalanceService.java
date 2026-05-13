package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.repository.RewardItemBalanceRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.result.PositionPeekItemBalanceResult;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPositionPeekItemBalanceService {
    public static final String POSITION_PEEK_ITEM_CODE = "position.peek";

    private final RewardShopItemRepository rewardShopItemRepository;
    private final RewardItemBalanceRepository rewardItemBalanceRepository;

    @Transactional(readOnly = true)
    public PositionPeekItemBalanceResult get(Long memberId) {
        RewardShopItem item = rewardShopItemRepository.findByCode(POSITION_PEEK_ITEM_CODE)
                .filter(RewardShopItem::positionPeek)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        return PositionPeekItemBalanceResult.from(
                item,
                rewardItemBalanceRepository.findByMemberIdAndShopItemId(memberId, item.id())
        );
    }
}
