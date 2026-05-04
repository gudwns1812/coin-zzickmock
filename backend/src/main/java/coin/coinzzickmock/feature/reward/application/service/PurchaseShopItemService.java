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
        if (itemCode == null || itemCode.isBlank()) {
            throw invalid("상점 상품 코드는 필수입니다.");
        }

        RewardShopItem requestedItem = rewardShopItemRepository.findByCode(itemCode)
                .orElseThrow(() -> invalid("존재하지 않는 상점 상품입니다."));
        if (!requestedItem.accountRefillCount()) {
            throw invalid("즉시 구매할 수 없는 상점 상품입니다.");
        }

        AccountRefillCreditResult refillCredit = accountRefillCreditProcessor.addTodayCount(memberId, 1);
        RewardShopItem item = rewardShopItemRepository.findByCodeForUpdate(itemCode)
                .orElseThrow(() -> invalid("존재하지 않는 상점 상품입니다."));
        if (!item.accountRefillCount()) {
            throw invalid("즉시 구매할 수 없는 상점 상품입니다.");
        }

        RewardShopMemberItemUsage usage = rewardShopMemberItemUsageRepository
                .findByMemberIdAndShopItemIdForUpdate(memberId, item.id())
                .orElse(RewardShopMemberItemUsage.empty(memberId, item.id()));
        validatePurchase(item, usage);

        RewardPointWallet wallet = rewardPointRepository.findByMemberIdForUpdate(memberId)
                .orElse(RewardPointWallet.empty(memberId));
        RewardPointWallet deductedWallet = wallet.deduct(item.price());
        RewardShopItem reservedItem = item.reserveOne();
        RewardShopMemberItemUsage reservedUsage = usage.increment();

        rewardShopItemRepository.save(reservedItem);
        rewardShopMemberItemUsageRepository.save(reservedUsage);
        RewardPointWallet savedWallet = rewardPointRepository.save(deductedWallet);

        String purchaseId = UUID.randomUUID().toString();
        rewardPointHistoryRepository.save(RewardPointHistory.instantShopPurchaseDeduct(
                memberId,
                item.price(),
                savedWallet.rewardPoint(),
                purchaseId
        ));

        return new ShopPurchaseResult(item.code(), savedWallet.rewardPoint(), refillCredit.remainingCount());
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

    private CoreException invalid(String message) {
        return new CoreException(ErrorCode.INVALID_REQUEST, message);
    }
}
