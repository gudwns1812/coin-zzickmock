package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardRedemptionRequestRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopMemberItemUsageRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardRedemptionResult;
import coin.coinzzickmock.feature.reward.application.result.ShopPurchaseResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistoryType;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import coin.coinzzickmock.feature.reward.domain.RewardShopMemberItemUsage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        classes = CoinZzickmockApplication.class,
        properties = {
                "spring.task.scheduling.enabled=false",
                "spring.mail.host="
        }
)
@ActiveProfiles("test")
@Transactional
class RewardRedemptionServiceTest {
    private static final Long MEMBER_ID = 1L;
    private static final Long ADMIN_ID = 1L;
    private static final String ITEM_CODE = "voucher.coffee";
    private static final String REFILL_ITEM_CODE = "account.refill-count";

    @Autowired
    private CreateRewardRedemptionService createRewardRedemptionService;

    @Autowired
    private AdminRewardRedemptionService adminRewardRedemptionService;

    @Autowired
    private GetRewardRedemptionHistoryService getRewardRedemptionHistoryService;

    @Autowired
    private CancelRewardRedemptionService cancelRewardRedemptionService;

    @Autowired
    private GetShopItemsService getShopItemsService;

    @Autowired
    private PurchaseShopItemService purchaseShopItemService;

    @Autowired
    private RewardPointRepository rewardPointRepository;

    @Autowired
    private RewardShopItemRepository rewardShopItemRepository;

    @Autowired
    private RewardShopMemberItemUsageRepository rewardShopMemberItemUsageRepository;

    @Autowired
    private RewardRedemptionRequestRepository rewardRedemptionRequestRepository;

    @Autowired
    private RewardPointHistoryRepository rewardPointHistoryRepository;

    @Test
    void createsPendingRequestAndConsumesPointStockAndMemberLimit() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 200));

        RewardRedemptionResult result = createRewardRedemptionService.create(MEMBER_ID, ITEM_CODE, "010-1234-5678");

        assertNotNull(result.requestId());
        assertEquals(RewardRedemptionStatus.PENDING, result.status());
        assertEquals(100, result.pointAmount());
        assertEquals(100, rewardPointRepository.findByMemberId(MEMBER_ID).orElseThrow().rewardPoint());
        RewardShopItem item = rewardShopItemRepository.findByCode(ITEM_CODE).orElseThrow();
        assertEquals(1, item.soldQuantity());
        RewardShopMemberItemUsage usage = rewardShopMemberItemUsageRepository
                .findByMemberIdAndShopItemIdForUpdate(MEMBER_ID, item.id())
                .orElseThrow();
        assertEquals(1, usage.purchaseCount());
        assertEquals(0, getShopItemsService.getItems(MEMBER_ID).get(0).remainingPurchaseLimit());
        List<RewardPointHistory> histories = rewardPointHistoryRepository.findByMemberId(MEMBER_ID);
        assertEquals(RewardPointHistoryType.REDEMPTION_DEDUCT, histories.get(0).historyType());
        assertEquals(-100, histories.get(0).amount());
        assertEquals(100, histories.get(0).balanceAfter());
    }

    @Test
    void invalidPhoneDoesNotMutateAccounting() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 200));

        CoreException thrown = assertThrows(CoreException.class,
                () -> createRewardRedemptionService.create(MEMBER_ID, ITEM_CODE, "010 1234 5678"));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        assertEquals(200, rewardPointRepository.findByMemberId(MEMBER_ID).orElseThrow().rewardPoint());
        assertEquals(0, rewardShopItemRepository.findByCode(ITEM_CODE).orElseThrow().soldQuantity());
        assertEquals(0, rewardRedemptionRequestRepository.findByStatus(RewardRedemptionStatus.PENDING).size());
    }

    @Test
    void purchasesRefillCountWithoutCreatingRedemptionRequest() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 50));

        ShopPurchaseResult result = purchaseShopItemService.purchase(MEMBER_ID, REFILL_ITEM_CODE);

        assertEquals(REFILL_ITEM_CODE, result.itemCode());
        assertEquals(30, result.rewardPoint());
        assertEquals(2, result.refillRemainingCount());
        assertEquals(30, rewardPointRepository.findByMemberId(MEMBER_ID).orElseThrow().rewardPoint());
        RewardShopItem item = rewardShopItemRepository.findByCode(REFILL_ITEM_CODE).orElseThrow();
        assertEquals(1, item.soldQuantity());
        assertEquals(0, rewardRedemptionRequestRepository.findByStatus(RewardRedemptionStatus.PENDING).size());
        RewardPointHistory history = rewardPointHistoryRepository.findByMemberId(MEMBER_ID).get(0);
        assertEquals(RewardPointHistoryType.REDEMPTION_DEDUCT, history.historyType());
        assertEquals(-20, history.amount());
        assertEquals("INSTANT_SHOP_PURCHASE", history.sourceType());
    }

    @Test
    void redemptionEndpointRejectsInstantPurchaseItemBeforePhoneValidation() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 50));

        CoreException thrown = assertThrows(CoreException.class,
                () -> createRewardRedemptionService.create(MEMBER_ID, REFILL_ITEM_CODE, "bad phone"));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        assertEquals(50, rewardPointRepository.findByMemberId(MEMBER_ID).orElseThrow().rewardPoint());
        assertEquals(0, rewardShopItemRepository.findByCode(REFILL_ITEM_CODE).orElseThrow().soldQuantity());
    }

    @Test
    void insufficientPointDoesNotMutateAccounting() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 99));

        CoreException thrown = assertThrows(CoreException.class,
                () -> createRewardRedemptionService.create(MEMBER_ID, ITEM_CODE, "010-1234-5678"));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        assertEquals(99, rewardPointRepository.findByMemberId(MEMBER_ID).orElseThrow().rewardPoint());
        assertEquals(0, rewardShopItemRepository.findByCode(ITEM_CODE).orElseThrow().soldQuantity());
        assertEquals(0, rewardPointHistoryRepository.findByMemberId(MEMBER_ID).size());
    }

    @Test
    void adminRejectRefundsExactlyOnceAndReleasesStockAndLimit() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 200));
        RewardRedemptionResult created = createRewardRedemptionService.create(MEMBER_ID, ITEM_CODE, "010-1234-5678");

        RewardRedemptionResult rejected = adminRewardRedemptionService.rejectAndRefund(
                created.requestId(),
                ADMIN_ID,
                "manual reject"
        );

        assertEquals(RewardRedemptionStatus.REJECTED, rejected.status());
        assertEquals(200, rewardPointRepository.findByMemberId(MEMBER_ID).orElseThrow().rewardPoint());
        RewardShopItem item = rewardShopItemRepository.findByCode(ITEM_CODE).orElseThrow();
        assertEquals(0, item.soldQuantity());
        RewardShopMemberItemUsage usage = rewardShopMemberItemUsageRepository
                .findByMemberIdAndShopItemIdForUpdate(MEMBER_ID, item.id())
                .orElseThrow();
        assertEquals(0, usage.purchaseCount());
        List<RewardPointHistory> histories = rewardPointHistoryRepository.findByMemberId(MEMBER_ID);
        assertEquals(RewardPointHistoryType.REDEMPTION_REFUND, histories.get(0).historyType());

        CoreException secondReject = assertThrows(CoreException.class,
                () -> adminRewardRedemptionService.rejectAndRefund(created.requestId(), ADMIN_ID, "again"));
        assertEquals(ErrorCode.REWARD_REDEMPTION_CONFLICT, secondReject.errorCode());
        assertEquals(200, rewardPointRepository.findByMemberId(MEMBER_ID).orElseThrow().rewardPoint());
    }

    @Test
    void ownerCancelRefundsOwnPendingRequestAndHistoryListsIt() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 200));
        RewardRedemptionResult created = createRewardRedemptionService.create(MEMBER_ID, ITEM_CODE, "010-1234-5678");

        RewardRedemptionResult cancelled = cancelRewardRedemptionService.cancel(MEMBER_ID, created.requestId());

        assertEquals(RewardRedemptionStatus.CANCELLED, cancelled.status());
        assertEquals(200, rewardPointRepository.findByMemberId(MEMBER_ID).orElseThrow().rewardPoint());
        assertEquals(1, getRewardRedemptionHistoryService.get(MEMBER_ID).size());
        CoreException secondCancel = assertThrows(CoreException.class,
                () -> cancelRewardRedemptionService.cancel(MEMBER_ID, created.requestId()));
        assertEquals(ErrorCode.REWARD_REDEMPTION_CONFLICT, secondCancel.errorCode());
    }

    @Test
    void ownerCancelMapsOtherMemberRequestToForbidden() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 200));
        RewardRedemptionResult created = createRewardRedemptionService.create(MEMBER_ID, ITEM_CODE, "010-1234-5678");

        CoreException thrown = assertThrows(CoreException.class,
                () -> cancelRewardRedemptionService.cancel(2L, created.requestId()));

        assertEquals(ErrorCode.FORBIDDEN, thrown.errorCode());
    }

    @Test
    void ownerCancelMapsMissingRequestToNotFound() {
        CoreException thrown = assertThrows(CoreException.class,
                () -> cancelRewardRedemptionService.cancel(MEMBER_ID, "missing-request"));

        assertEquals(ErrorCode.REWARD_REDEMPTION_NOT_FOUND, thrown.errorCode());
    }

    @Test
    void approvedRequestCannotBeRejected() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 200));
        RewardRedemptionResult created = createRewardRedemptionService.create(MEMBER_ID, ITEM_CODE, "010-1234-5678");

        adminRewardRedemptionService.approve(created.requestId(), ADMIN_ID, "approved");

        CoreException thrown = assertThrows(CoreException.class,
                () -> adminRewardRedemptionService.rejectAndRefund(created.requestId(), ADMIN_ID, "too late"));
        assertEquals(ErrorCode.REWARD_REDEMPTION_CONFLICT, thrown.errorCode());
        assertEquals(100, rewardPointRepository.findByMemberId(MEMBER_ID).orElseThrow().rewardPoint());
        assertEquals(1, rewardShopItemRepository.findByCode(ITEM_CODE).orElseThrow().soldQuantity());
    }

    @Test
    void adminApproveMapsMissingRequestToNotFound() {
        CoreException thrown = assertThrows(CoreException.class,
                () -> adminRewardRedemptionService.approve("missing-request", ADMIN_ID, "missing"));

        assertEquals(ErrorCode.REWARD_REDEMPTION_NOT_FOUND, thrown.errorCode());
    }
}
