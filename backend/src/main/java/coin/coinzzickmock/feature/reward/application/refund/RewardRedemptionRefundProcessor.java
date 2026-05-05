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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> invalid());
        RewardShopMemberItemUsage usage = rewardShopMemberItemUsageRepository
                .findByMemberIdAndShopItemIdForUpdate(request.memberId(), request.shopItemId())
                .orElseThrow(() -> invalid());
        RewardPointWallet wallet = rewardPointRepository.findByMemberIdForUpdate(request.memberId())
                .orElseThrow(() -> invalid());

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

    private CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
