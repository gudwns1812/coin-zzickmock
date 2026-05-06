package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.refill.AccountRefillCreditProcessor;
import coin.coinzzickmock.feature.account.application.result.AccountRefillCreditResult;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopMemberItemUsageRepository;
import coin.coinzzickmock.feature.reward.application.result.ShopPurchaseResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import coin.coinzzickmock.feature.reward.domain.RewardShopMemberItemUsage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseShopItemService {
    private final RewardShopItemRepository rewardShopItemRepository;
    private final RewardShopMemberItemUsageRepository rewardShopMemberItemUsageRepository;
    private final RewardPointRepository rewardPointRepository;
    private final RewardPointHistoryRepository rewardPointHistoryRepository;
    private final AccountRefillCreditProcessor accountRefillCreditProcessor;

    @Transactional
    public ShopPurchaseResult purchase(Long memberId, String itemCode) {
        String normalizedItemCode = requireItemCode(itemCode);
        RewardShopItem item = loadPurchasableRefillItemForUpdate(normalizedItemCode);
        RewardShopMemberItemUsage usage = loadUsageForUpdate(memberId, item);
        validatePurchase(item, usage);

        RewardPointWallet deductedWallet = deductWallet(memberId, item.price());
        AccountRefillCreditResult refillCredit = applyRefillCredit(memberId);
        ReservedShopPurchase reservedPurchase = reserveItemAndUsage(item, usage);

        persistReservation(reservedPurchase);
        RewardPointWallet savedWallet = rewardPointRepository.save(deductedWallet);
        recordHistory(memberId, item.price(), savedWallet.rewardPoint());

        return new ShopPurchaseResult(normalizedItemCode, savedWallet.rewardPoint(), refillCredit.remainingCount());
    }

    private String requireItemCode(String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            throw invalid();
        }
        return itemCode;
    }

    private AccountRefillCreditResult applyRefillCredit(Long memberId) {
        return accountRefillCreditProcessor.addTodayCount(memberId, 1);
    }

    private RewardShopItem loadPurchasableRefillItemForUpdate(String itemCode) {
        RewardShopItem item = rewardShopItemRepository.findByCodeForUpdate(itemCode)
                .orElseThrow(this::invalid);
        if (!item.accountRefillCount()) {
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

    private ReservedShopPurchase reserveItemAndUsage(RewardShopItem item, RewardShopMemberItemUsage usage) {
        return new ReservedShopPurchase(item.reserveOne(), usage.increment());
    }

    private void persistReservation(ReservedShopPurchase reservedPurchase) {
        rewardShopItemRepository.save(reservedPurchase.item());
        rewardShopMemberItemUsageRepository.save(reservedPurchase.usage());
    }

    private void recordHistory(Long memberId, int price, int balanceAfter) {
        rewardPointHistoryRepository.save(RewardPointHistory.instantShopPurchaseDeduct(
                memberId,
                price,
                balanceAfter,
                UUID.randomUUID().toString()
        ));
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

    private record ReservedShopPurchase(
            RewardShopItem item,
            RewardShopMemberItemUsage usage
    ) {
    }
}
