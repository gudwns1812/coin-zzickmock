package coin.coinzzickmock.feature.reward.application.refund;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopMemberItemUsageRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import coin.coinzzickmock.feature.reward.domain.RewardShopMemberItemUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RewardRedemptionRefundProcessor {
    private final RewardShopItemRepository rewardShopItemRepository;
    private final RewardShopMemberItemUsageRepository rewardShopMemberItemUsageRepository;
    private final RewardPointRepository rewardPointRepository;
    private final RewardPointHistoryRepository rewardPointHistoryRepository;

    @Transactional
    public void refundReservation(RewardRedemptionRequest request) {
        RewardShopItem item = rewardShopItemRepository.findByIdForUpdate(request.shopItemId())
                .orElseThrow(() -> internal("shop_item_missing"));
        RewardShopMemberItemUsage usage = rewardShopMemberItemUsageRepository
                .findByMemberIdAndShopItemIdForUpdate(request.memberId(), request.shopItemId())
                .orElseThrow(() -> internal("member_item_usage_missing"));
        RewardPointWallet wallet = rewardPointRepository.findByMemberIdForUpdate(request.memberId())
                .orElseThrow(() -> internal("reward_wallet_missing"));

        RewardShopItem releasedItem = item.releaseOne();
        RewardShopMemberItemUsage releasedUsage = usage.decrement();
        RewardPointWallet refundedWallet = wallet.refund(request.pointAmount());

        rewardShopItemRepository.save(releasedItem);
        rewardShopMemberItemUsageRepository.save(releasedUsage);
        RewardPointWallet savedWallet = rewardPointRepository.save(refundedWallet);
        rewardPointHistoryRepository.save(RewardPointHistory.redemptionRefund(
                request.memberId(),
                request.pointAmount(),
                savedWallet.rewardPoint(),
                request.requestId()
        ));
    }

    private CoreException internal(String reason) {
        log.error("Reward redemption refund state was inconsistent. operation=refund_reservation reason={}", reason);
        return new CoreException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
