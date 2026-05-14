package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.refill.AccountRefillCreditProcessor;
import coin.coinzzickmock.feature.account.application.result.AccountRefillCreditResult;
import coin.coinzzickmock.feature.reward.application.repository.RewardItemBalanceRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopPurchaseRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopMemberItemUsageRepository;
import coin.coinzzickmock.feature.reward.application.result.ShopPurchaseResult;
import coin.coinzzickmock.feature.reward.domain.RewardItemBalance;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.domain.RewardShopPurchase;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import coin.coinzzickmock.feature.reward.domain.RewardShopMemberItemUsage;
import java.time.Instant;
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
    private final RewardItemBalanceRepository rewardItemBalanceRepository;
    private final RewardShopPurchaseRepository rewardShopPurchaseRepository;

    @Transactional
    public ShopPurchaseResult purchase(Long memberId, String itemCode) {
        String normalizedItemCode = requireItemCode(itemCode);
        RewardShopItem item = loadPurchasableInstantItemForUpdate(normalizedItemCode);
        RewardShopMemberItemUsage usage = loadUsageForUpdate(memberId, item);
        validatePurchase(item, usage);
        String purchaseId = UUID.randomUUID().toString();
        Instant purchasedAt = Instant.now();

        RewardPointWallet deductedWallet = deductWallet(memberId, item.price());
        ReservedShopPurchase reservedPurchase = reserveItemAndUsage(item, usage);
        ShopPurchaseEffect effect = applyPurchaseEffect(memberId, item);

        persistReservation(reservedPurchase);
        RewardPointWallet savedWallet = rewardPointRepository.save(deductedWallet);
        recordPurchase(purchaseId, memberId, item, purchasedAt);
        recordHistory(memberId, item.price(), savedWallet.rewardPoint(), purchaseId);

        return effect.toResult(normalizedItemCode, savedWallet.rewardPoint());
    }

    private String requireItemCode(String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            throw invalid();
        }
        return itemCode;
    }

    private RewardShopItem loadPurchasableInstantItemForUpdate(String itemCode) {
        RewardShopItem item = rewardShopItemRepository.findByCodeForUpdate(itemCode)
                .orElseThrow(this::invalid);
        if (!item.instantConsumable()) {
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

    private ShopPurchaseEffect applyPurchaseEffect(Long memberId, RewardShopItem item) {
        if (item.accountRefillCount()) {
            AccountRefillCreditResult refillCredit = accountRefillCreditProcessor.addCurrentWeekCount(memberId, 1);
            return ShopPurchaseEffect.accountRefill(refillCredit.remainingCount());
        }
        if (item.positionPeek()) {
            RewardItemBalance balance = rewardItemBalanceRepository
                    .findByMemberIdAndShopItemIdForUpdate(memberId, item.id())
                    .orElse(RewardItemBalance.empty(memberId, item.id()))
                    .addOne();
            RewardItemBalance saved = rewardItemBalanceRepository.save(balance);
            return ShopPurchaseEffect.positionPeek(saved.remainingQuantity());
        }
        throw invalid();
    }

    private ReservedShopPurchase reserveItemAndUsage(RewardShopItem item, RewardShopMemberItemUsage usage) {
        return new ReservedShopPurchase(item.reserveOne(), usage.increment());
    }

    private void persistReservation(ReservedShopPurchase reservedPurchase) {
        rewardShopItemRepository.save(reservedPurchase.item());
        rewardShopMemberItemUsageRepository.save(reservedPurchase.usage());
    }

    private void recordPurchase(String purchaseId, Long memberId, RewardShopItem item, Instant purchasedAt) {
        rewardShopPurchaseRepository.create(RewardShopPurchase.instant(purchaseId, memberId, item, purchasedAt));
    }

    private void recordHistory(Long memberId, int price, int balanceAfter, String purchaseId) {
        rewardPointHistoryRepository.save(RewardPointHistory.instantShopPurchaseDeduct(
                memberId,
                price,
                balanceAfter,
                purchaseId
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

    private record ShopPurchaseEffect(
            Integer refillRemainingCount,
            Integer positionPeekItemBalance
    ) {
        static ShopPurchaseEffect accountRefill(int refillRemainingCount) {
            return new ShopPurchaseEffect(refillRemainingCount, null);
        }

        static ShopPurchaseEffect positionPeek(int positionPeekItemBalance) {
            return new ShopPurchaseEffect(null, positionPeekItemBalance);
        }

        ShopPurchaseResult toResult(String itemCode, int rewardPoint) {
            if (refillRemainingCount != null) {
                return ShopPurchaseResult.accountRefill(itemCode, rewardPoint, refillRemainingCount);
            }
            if (positionPeekItemBalance != null) {
                return ShopPurchaseResult.positionPeek(itemCode, rewardPoint, positionPeekItemBalance);
            }
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
