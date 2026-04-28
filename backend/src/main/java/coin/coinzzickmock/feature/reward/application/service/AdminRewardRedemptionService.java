package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardRedemptionRequestRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopMemberItemUsageRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardRedemptionResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import coin.coinzzickmock.feature.reward.domain.RewardShopMemberItemUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRewardRedemptionService {
    private final RewardRedemptionRequestRepository rewardRedemptionRequestRepository;
    private final RewardShopItemRepository rewardShopItemRepository;
    private final RewardShopMemberItemUsageRepository rewardShopMemberItemUsageRepository;
    private final RewardPointRepository rewardPointRepository;
    private final RewardPointHistoryRepository rewardPointHistoryRepository;

    @Transactional(readOnly = true)
    public List<RewardRedemptionResult> list(RewardRedemptionStatus status) {
        return rewardRedemptionRequestRepository.findByStatus(status).stream()
                .map(RewardRedemptionResult::from)
                .toList();
    }

    @Transactional
    public RewardRedemptionResult markSent(String requestId, String adminMemberId, String adminMemo) {
        RewardRedemptionRequest request = findPendingRequest(requestId);
        RewardRedemptionRequest sent = transition(() -> request.markSent(adminMemberId, adminMemo, Instant.now()));
        return RewardRedemptionResult.from(rewardRedemptionRequestRepository.save(sent));
    }

    @Transactional
    public RewardRedemptionResult cancelAndRefund(String requestId, String adminMemberId, String adminMemo) {
        RewardRedemptionRequest request = findPendingRequest(requestId);
        RewardShopItem item = rewardShopItemRepository.findByIdForUpdate(request.shopItemId())
                .orElseThrow(() -> invalid("상점 상품을 찾을 수 없습니다."));
        RewardShopMemberItemUsage usage = rewardShopMemberItemUsageRepository
                .findByMemberIdAndShopItemIdForUpdate(request.memberId(), request.shopItemId())
                .orElseThrow(() -> invalid("구매 제한 사용량을 찾을 수 없습니다."));
        RewardPointWallet wallet = rewardPointRepository.findByMemberIdForUpdate(request.memberId())
                .orElse(RewardPointWallet.empty(request.memberId()));

        RewardRedemptionRequest cancelled = transition(() -> request.cancelRefunded(adminMemberId, adminMemo, Instant.now()));
        RewardShopItem releasedItem = transition(item::releaseOne);
        RewardShopMemberItemUsage releasedUsage = transition(usage::decrement);
        RewardPointWallet refundedWallet = transition(() -> wallet.refund(request.pointAmount()));

        rewardShopItemRepository.save(releasedItem);
        rewardShopMemberItemUsageRepository.save(releasedUsage);
        RewardPointWallet savedWallet = rewardPointRepository.save(refundedWallet);
        RewardRedemptionRequest savedRequest = rewardRedemptionRequestRepository.save(cancelled);
        rewardPointHistoryRepository.save(RewardPointHistory.redemptionRefund(
                request.memberId(),
                request.pointAmount(),
                savedWallet.rewardPoint(),
                request.requestId()
        ));
        return RewardRedemptionResult.from(savedRequest);
    }

    private RewardRedemptionRequest findPendingRequest(String requestId) {
        RewardRedemptionRequest request = rewardRedemptionRequestRepository.findByRequestIdForUpdate(requestId)
                .orElseThrow(() -> invalid("교환권 요청을 찾을 수 없습니다."));
        if (request.status() != RewardRedemptionStatus.PENDING) {
            throw invalid("대기 중인 교환권 요청만 처리할 수 있습니다.");
        }
        return request;
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
