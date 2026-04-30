package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.event.RewardRedemptionCreatedEvent;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardRedemptionRequestRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopMemberItemUsageRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardRedemptionResult;
import coin.coinzzickmock.feature.reward.domain.RewardPhoneNumber;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import coin.coinzzickmock.feature.reward.domain.RewardShopMemberItemUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateRewardRedemptionService {
    private final RewardShopItemRepository rewardShopItemRepository;
    private final RewardShopMemberItemUsageRepository rewardShopMemberItemUsageRepository;
    private final RewardPointRepository rewardPointRepository;
    private final RewardPointHistoryRepository rewardPointHistoryRepository;
    private final RewardRedemptionRequestRepository rewardRedemptionRequestRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    @Transactional
    public RewardRedemptionResult create(Long memberId, String itemCode, String phoneNumber) {
        RewardPhoneNumber normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        if (itemCode == null || itemCode.isBlank()) {
            throw invalid("상점 상품 코드는 필수입니다.");
        }
        RewardShopItem item = rewardShopItemRepository.findByCodeForUpdate(itemCode)
                .orElseThrow(() -> invalid("존재하지 않는 상점 상품입니다."));
        RewardShopMemberItemUsage usage = rewardShopMemberItemUsageRepository
                .findByMemberIdAndShopItemIdForUpdate(memberId, item.id())
                .orElse(RewardShopMemberItemUsage.empty(memberId, item.id()));
        validatePurchase(item, usage);

        RewardPointWallet wallet = rewardPointRepository.findByMemberIdForUpdate(memberId)
                .orElse(RewardPointWallet.empty(memberId));
        RewardPointWallet deductedWallet = deduct(wallet, item.price());
        String requestId = UUID.randomUUID().toString();
        RewardRedemptionRequest request = RewardRedemptionRequest.pending(
                requestId,
                memberId,
                item,
                normalizedPhoneNumber,
                Instant.now()
        );

        RewardShopItem reservedItem = reserve(item);
        RewardShopMemberItemUsage reservedUsage = usage.increment();

        rewardShopItemRepository.save(reservedItem);
        rewardShopMemberItemUsageRepository.save(reservedUsage);
        RewardPointWallet savedWallet = rewardPointRepository.save(deductedWallet);
        RewardRedemptionRequest savedRequest = rewardRedemptionRequestRepository.save(request);
        rewardPointHistoryRepository.save(RewardPointHistory.redemptionDeduct(
                memberId,
                item.price(),
                savedWallet.rewardPoint(),
                requestId
        ));
        afterCommitEventPublisher.publish(RewardRedemptionCreatedEvent.from(savedRequest));
        return RewardRedemptionResult.from(savedRequest);
    }

    private RewardPhoneNumber normalizePhoneNumber(String phoneNumber) {
        try {
            return RewardPhoneNumber.from(phoneNumber);
        } catch (IllegalArgumentException exception) {
            throw invalid(exception.getMessage());
        }
    }

    private void validatePurchase(RewardShopItem item, RewardShopMemberItemUsage usage) {
        if (!item.active()) {
            throw invalid("비활성 상품은 구매할 수 없습니다.");
        }
        if (item.soldOut()) {
            throw invalid("품절된 상품입니다.");
        }
        if (item.memberReachedLimit(usage.purchaseCount())) {
            throw invalid("회원별 구매 제한에 도달했습니다.");
        }
    }

    private RewardPointWallet deduct(RewardPointWallet wallet, int pointAmount) {
        try {
            return wallet.deduct(pointAmount);
        } catch (IllegalArgumentException exception) {
            throw invalid(exception.getMessage());
        }
    }

    private RewardShopItem reserve(RewardShopItem item) {
        try {
            return item.reserveOne();
        } catch (IllegalStateException exception) {
            throw invalid(exception.getMessage());
        }
    }

    private CoreException invalid(String message) {
        return new CoreException(ErrorCode.INVALID_REQUEST, message);
    }
}
