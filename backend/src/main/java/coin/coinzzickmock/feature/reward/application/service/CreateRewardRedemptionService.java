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
        String normalizedItemCode = requireItemCode(itemCode);
        RewardShopItem item = loadCoffeeVoucherForUpdate(normalizedItemCode);
        RewardPhoneNumber normalizedPhoneNumber = RewardPhoneNumber.from(phoneNumber);
        RewardShopMemberItemUsage usage = loadUsageForUpdate(memberId, item);
        validatePurchase(item, usage);

        RewardPointWallet deductedWallet = deductWallet(memberId, item.price());
        RewardRedemptionRequest request = createPendingRequest(memberId, item, normalizedPhoneNumber);
        ReservedRewardRedemption reservedRedemption = reserveItemAndUsage(item, usage);

        persistReservation(reservedRedemption);
        RewardPointWallet savedWallet = rewardPointRepository.save(deductedWallet);
        RewardRedemptionRequest savedRequest = rewardRedemptionRequestRepository.save(request);
        recordHistory(memberId, item.price(), savedWallet.rewardPoint(), savedRequest.requestId());
        publishRedemptionCreated(savedRequest);
        return RewardRedemptionResult.from(savedRequest);
    }

    private String requireItemCode(String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            throw invalid();
        }
        return itemCode;
    }

    private RewardShopItem loadCoffeeVoucherForUpdate(String itemCode) {
        RewardShopItem item = rewardShopItemRepository.findByCodeForUpdate(itemCode)
                .orElseThrow(this::invalid);
        if (!item.coffeeVoucher()) {
            throw invalid();
        }
        return item;
    }

    private RewardShopMemberItemUsage loadUsageForUpdate(Long memberId, RewardShopItem item) {
        return rewardShopMemberItemUsageRepository
                .findByMemberIdAndShopItemIdForUpdate(memberId, item.id())
                .orElse(RewardShopMemberItemUsage.empty(memberId, item.id()));
    }

    private RewardPointWallet deductWallet(Long memberId, int price) {
        RewardPointWallet wallet = rewardPointRepository.findByMemberIdForUpdate(memberId)
                .orElse(RewardPointWallet.empty(memberId));
        return wallet.deduct(price);
    }

    private RewardRedemptionRequest createPendingRequest(
            Long memberId,
            RewardShopItem item,
            RewardPhoneNumber phoneNumber
    ) {
        return RewardRedemptionRequest.pending(
                UUID.randomUUID().toString(),
                memberId,
                item,
                phoneNumber,
                Instant.now()
        );
    }

    private ReservedRewardRedemption reserveItemAndUsage(RewardShopItem item, RewardShopMemberItemUsage usage) {
        return new ReservedRewardRedemption(item.reserveOne(), usage.increment());
    }

    private void persistReservation(ReservedRewardRedemption reservedRedemption) {
        rewardShopItemRepository.save(reservedRedemption.item());
        rewardShopMemberItemUsageRepository.save(reservedRedemption.usage());
    }

    private void recordHistory(Long memberId, int price, int balanceAfter, String requestId) {
        rewardPointHistoryRepository.save(RewardPointHistory.redemptionDeduct(
                memberId,
                price,
                balanceAfter,
                requestId
        ));
    }

    private void publishRedemptionCreated(RewardRedemptionRequest savedRequest) {
        afterCommitEventPublisher.publish(RewardRedemptionCreatedEvent.from(savedRequest));
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

    private record ReservedRewardRedemption(
            RewardShopItem item,
            RewardShopMemberItemUsage usage
    ) {
    }
}
