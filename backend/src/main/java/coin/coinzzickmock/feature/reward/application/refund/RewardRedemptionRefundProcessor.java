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
                .orElseThrow(() -> invalid("상점 상품을 찾을 수 없습니다."));
        RewardShopMemberItemUsage usage = rewardShopMemberItemUsageRepository
                .findByMemberIdAndShopItemIdForUpdate(request.memberId(), request.shopItemId())
                .orElseThrow(() -> invalid("구매 제한 사용량을 찾을 수 없습니다."));
        RewardPointWallet wallet = rewardPointRepository.findByMemberIdForUpdate(request.memberId())
                .orElseThrow(() -> invalid("포인트 지갑을 찾을 수 없습니다."));

        RewardShopItem releasedItem = transition(item::releaseOne);
        RewardShopMemberItemUsage releasedUsage = transition(usage::decrement);
        RewardPointWallet refundedWallet = transition(() -> wallet.refund(request.pointAmount()));

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

    private <T> T transition(Transition<T> transition) {
        try {
            return transition.run();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw invalid(exception.getMessage());
        }
    }

    private CoreException invalid(String message) {
        return new CoreException(ErrorCode.INVALID_REQUEST, message);
    }

    @FunctionalInterface
    private interface Transition<T> {
        T run();
    }
}
