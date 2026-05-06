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
        if (itemCode == null || itemCode.isBlank()) {
            throw invalid();
        }
        RewardShopItem item = rewardShopItemRepository.findByCodeForUpdate(itemCode)
                .orElseThrow(() -> invalid());
        if (!item.coffeeVoucher()) {
            throw invalid();
        }

        RewardPhoneNumber normalizedPhoneNumber = RewardPhoneNumber.from(phoneNumber);
        RewardShopMemberItemUsage usage = rewardShopMemberItemUsageRepository
                .findByMemberIdAndShopItemIdForUpdate(memberId, item.id())
                .orElse(RewardShopMemberItemUsage.empty(memberId, item.id()));
        validatePurchase(item, usage);

        RewardPointWallet wallet = rewardPointRepository.findByMemberIdForUpdate(memberId)
                .orElse(RewardPointWallet.empty(memberId));
        RewardPointWallet deductedWallet = wallet.deduct(item.price());
        String requestId = UUID.randomUUID().toString();
        RewardRedemptionRequest request = RewardRedemptionRequest.pending(
                requestId,
                memberId,
                item,
                normalizedPhoneNumber,
                Instant.now()
        );

        RewardShopItem reservedItem = item.reserveOne();
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

    private void validatePurchase(RewardShopItem item, RewardShopMemberItemUsage usage) {
        if (!item.active()) {
            throw invalid();
        }
        if (item.soldOut()) {
            throw invalid();
        }
        if (item.memberReachedLimit(usage.purchaseCount())) {
            throw invalid();
        }
    }

    private CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
