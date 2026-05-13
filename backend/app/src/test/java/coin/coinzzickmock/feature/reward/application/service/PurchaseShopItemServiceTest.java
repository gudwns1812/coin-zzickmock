package coin.coinzzickmock.feature.reward.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.refill.AccountRefillCreditProcessor;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository.LockedAccountRefillState;
import coin.coinzzickmock.feature.account.application.service.AccountRefillDatePolicy;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import coin.coinzzickmock.feature.reward.application.repository.RewardItemBalanceRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopMemberItemUsageRepository;
import coin.coinzzickmock.feature.reward.application.result.ShopPurchaseResult;
import coin.coinzzickmock.feature.reward.domain.RewardItemBalance;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import coin.coinzzickmock.feature.reward.domain.RewardShopMemberItemUsage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PurchaseShopItemServiceTest {
    private static final Long MEMBER_ID = 1L;
    private static final String REFILL_ITEM_CODE = "account.refill-count";
    private static final String COFFEE_ITEM_CODE = "voucher.coffee";
    private static final String POSITION_PEEK_ITEM_CODE = "position.peek";

    @Test
    void validatesRewardRowsBeforeCreditingRefillState() {
        List<String> lockOrder = new ArrayList<>();
        RecordingRefillStateRepository refillStateRepository = new RecordingRefillStateRepository(lockOrder);
        PurchaseShopItemService service = new PurchaseShopItemService(
                new RecordingShopItemRepository(lockOrder),
                new RecordingUsageRepository(lockOrder),
                new RecordingPointRepository(lockOrder),
                new coin.coinzzickmock.testsupport.TestRewardPointHistoryRepository() {
                    @Override
                    public RewardPointHistory save(RewardPointHistory history) {
                        return history;
                    }
                },
                new AccountRefillCreditProcessor(refillStateRepository, new AccountRefillDatePolicy()),
                new RecordingItemBalanceRepository(lockOrder)
        );

        ShopPurchaseResult result = service.purchase(1L, "account.refill-count");

        assertEquals(80, result.rewardPoint());
        assertEquals(2, result.refillRemainingCount());
        assertTrue(lockOrder.contains("refill-state"), "refill-state lock should be acquired");
        assertTrue(lockOrder.contains("shop-item"), "shop-item lock should be acquired");
        assertTrue(lockOrder.contains("usage"), "usage lock should be acquired");
        assertTrue(lockOrder.contains("wallet"), "wallet lock should be acquired");
        assertTrue(lockOrder.indexOf("shop-item") < lockOrder.indexOf("wallet"));
        assertTrue(lockOrder.indexOf("usage") < lockOrder.indexOf("wallet"));
        assertTrue(lockOrder.indexOf("wallet") < lockOrder.indexOf("refill-state"));
    }

    @Test
    void purchaseDeductsWalletAppliesRefillCreditAndPersistsHistory() {
        List<String> lockOrder = new ArrayList<>();
        RecordingShopItemRepository shopItemRepository = new RecordingShopItemRepository(lockOrder);
        RecordingUsageRepository usageRepository = new RecordingUsageRepository(lockOrder);
        RecordingPointRepository pointRepository = new RecordingPointRepository(lockOrder);
        RecordingPointHistoryRepository historyRepository = new RecordingPointHistoryRepository();
        RecordingRefillStateRepository refillStateRepository = new RecordingRefillStateRepository(lockOrder);
        RecordingItemBalanceRepository itemBalanceRepository = new RecordingItemBalanceRepository(lockOrder);
        PurchaseShopItemService service = service(
                shopItemRepository,
                usageRepository,
                pointRepository,
                historyRepository,
                refillStateRepository,
                itemBalanceRepository
        );

        ShopPurchaseResult result = service.purchase(MEMBER_ID, REFILL_ITEM_CODE);

        assertEquals(REFILL_ITEM_CODE, result.itemCode());
        assertEquals(80, result.rewardPoint());
        assertEquals(2, result.refillRemainingCount());
        assertEquals(1, shopItemRepository.item().soldQuantity());
        assertEquals(1, usageRepository.usage().purchaseCount());
        assertEquals(80, pointRepository.wallet().rewardPoint());
        assertEquals(2, refillStateRepository.state().remainingCount());
        assertFalse(itemBalanceRepository.hasBalance());
        RewardPointHistory history = historyRepository.histories().get(0);
        assertEquals(-20, history.amount());
        assertEquals(80, history.balanceAfter());
        assertEquals("INSTANT_SHOP_PURCHASE", history.sourceType());
    }

    @Test
    void invalidItemDoesNotMutateWalletRefillCreditsOrRewardRows() {
        List<String> lockOrder = new ArrayList<>();
        RecordingShopItemRepository shopItemRepository = new RecordingShopItemRepository(lockOrder, coffeeVoucher());
        RecordingUsageRepository usageRepository = new RecordingUsageRepository(lockOrder);
        RecordingPointRepository pointRepository = new RecordingPointRepository(lockOrder);
        RecordingPointHistoryRepository historyRepository = new RecordingPointHistoryRepository();
        RecordingRefillStateRepository refillStateRepository = new RecordingRefillStateRepository(lockOrder);
        RecordingItemBalanceRepository itemBalanceRepository = new RecordingItemBalanceRepository(lockOrder);
        PurchaseShopItemService service = service(
                shopItemRepository,
                usageRepository,
                pointRepository,
                historyRepository,
                refillStateRepository,
                itemBalanceRepository
        );

        CoreException thrown = assertThrows(CoreException.class,
                () -> service.purchase(MEMBER_ID, COFFEE_ITEM_CODE));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        assertEquals(100, pointRepository.wallet().rewardPoint());
        assertEquals(0, shopItemRepository.item().soldQuantity());
        assertFalse(refillStateRepository.hasState());
        assertFalse(usageRepository.hasUsage());
        assertFalse(itemBalanceRepository.hasBalance());
        assertEquals(0, historyRepository.histories().size());
        assertFalse(lockOrder.contains("refill-state"));
        assertFalse(lockOrder.contains("wallet"));
    }

    @Test
    void positionPeekPurchaseDeductsWalletAndIncreasesItemBalanceWithoutRefillCredit() {
        List<String> lockOrder = new ArrayList<>();
        RecordingShopItemRepository shopItemRepository = new RecordingShopItemRepository(lockOrder, positionPeekItem());
        RecordingUsageRepository usageRepository = new RecordingUsageRepository(lockOrder);
        RecordingPointRepository pointRepository = new RecordingPointRepository(lockOrder);
        RecordingPointHistoryRepository historyRepository = new RecordingPointHistoryRepository();
        RecordingRefillStateRepository refillStateRepository = new RecordingRefillStateRepository(lockOrder);
        RecordingItemBalanceRepository itemBalanceRepository = new RecordingItemBalanceRepository(lockOrder);
        PurchaseShopItemService service = service(
                shopItemRepository,
                usageRepository,
                pointRepository,
                historyRepository,
                refillStateRepository,
                itemBalanceRepository
        );

        ShopPurchaseResult result = service.purchase(MEMBER_ID, POSITION_PEEK_ITEM_CODE);

        assertEquals(POSITION_PEEK_ITEM_CODE, result.itemCode());
        assertEquals(70, result.rewardPoint());
        assertEquals(null, result.refillRemainingCount());
        assertEquals(1, result.positionPeekItemBalance());
        assertEquals(1, shopItemRepository.item().soldQuantity());
        assertEquals(1, usageRepository.usage().purchaseCount());
        assertEquals(1, itemBalanceRepository.balance().remainingQuantity());
        assertFalse(refillStateRepository.hasState());
        assertTrue(lockOrder.contains("item-balance"));
        assertTrue(lockOrder.indexOf("wallet") < lockOrder.indexOf("item-balance"));
        RewardPointHistory history = historyRepository.histories().get(0);
        assertEquals(-30, history.amount());
        assertEquals(70, history.balanceAfter());
    }

    private PurchaseShopItemService service(
            RecordingShopItemRepository shopItemRepository,
            RecordingUsageRepository usageRepository,
            RecordingPointRepository pointRepository,
            RecordingPointHistoryRepository historyRepository,
            RecordingRefillStateRepository refillStateRepository,
            RecordingItemBalanceRepository itemBalanceRepository
    ) {
        return new PurchaseShopItemService(
                shopItemRepository,
                usageRepository,
                pointRepository,
                historyRepository,
                new AccountRefillCreditProcessor(refillStateRepository, new AccountRefillDatePolicy()),
                itemBalanceRepository
        );
    }

    private static class RecordingRefillStateRepository implements AccountRefillStateRepository {
        private final List<String> lockOrder;
        private AccountRefillState state;

        private RecordingRefillStateRepository(List<String> lockOrder) {
            this.lockOrder = lockOrder;
        }

        @Override
        public Optional<AccountRefillState> findByMemberIdAndRefillDate(Long memberId, LocalDate refillDate) {
            return Optional.ofNullable(state);
        }

        @Override
        public void provisionWeeklyStateIfAbsent(Long memberId, LocalDate refillDate) {
            lockOrder.add("refill-state");
            if (state == null) {
                state = AccountRefillState.weekly(memberId, refillDate);
            }
        }

        @Override
        public AccountRefillState grantExtraRefillCount(Long memberId, LocalDate refillDate, int count) {
            lockOrder.add("refill-state");
            if (state == null) {
                state = AccountRefillState.weekly(memberId, refillDate);
            }
            state = state.addCount(count).withVersion(state.version() + 1);
            return state;
        }

        @Override
        public Optional<LockedAccountRefillState> findByMemberIdAndRefillDateForUpdate(Long memberId, LocalDate refillDate) {
            return Optional.ofNullable(state).map(ignored -> new InMemoryLockedAccountRefillState());
        }

        private class InMemoryLockedAccountRefillState implements LockedAccountRefillState {
            @Override
            public AccountRefillState state() {
                return state;
            }

            @Override
            public AccountRefillState consumeOne() {
                state = state.consumeOne().withVersion(state.version() + 1);
                return state;
            }
        }

        private boolean hasState() {
            return state != null;
        }

        private AccountRefillState state() {
            return state;
        }
    }

    private static class RecordingShopItemRepository implements RewardShopItemRepository {
        private final List<String> lockOrder;
        private RewardShopItem item;

        private RecordingShopItemRepository(List<String> lockOrder) {
            this(lockOrder, refillCountItem());
        }

        private RecordingShopItemRepository(List<String> lockOrder, RewardShopItem item) {
            this.lockOrder = lockOrder;
            this.item = item;
        }

        @Override
        public List<RewardShopItem> findAllItems() {
            return List.of(item);
        }

        @Override
        public List<RewardShopItem> findActiveItems() {
            return List.of(item);
        }

        @Override
        public Optional<RewardShopItem> findByCode(String code) {
            return Optional.of(item);
        }

        @Override
        public Optional<RewardShopItem> findByCodeForUpdate(String code) {
            lockOrder.add("shop-item");
            return Optional.of(item);
        }

        @Override
        public Optional<RewardShopItem> findByIdForUpdate(Long id) {
            return Optional.of(item);
        }

        @Override
        public RewardShopItem save(RewardShopItem item) {
            this.item = item;
            return item;
        }

        private RewardShopItem item() {
            return item;
        }
    }

    private static class RecordingUsageRepository implements RewardShopMemberItemUsageRepository {
        private final List<String> lockOrder;
        private RewardShopMemberItemUsage usage;

        private RecordingUsageRepository(List<String> lockOrder) {
            this.lockOrder = lockOrder;
        }

        @Override
        public Optional<RewardShopMemberItemUsage> findByMemberIdAndShopItemId(Long memberId, Long shopItemId) {
            return Optional.ofNullable(usage);
        }

        @Override
        public Optional<RewardShopMemberItemUsage> findByMemberIdAndShopItemIdForUpdate(Long memberId, Long shopItemId) {
            lockOrder.add("usage");
            return Optional.ofNullable(usage);
        }

        @Override
        public RewardShopMemberItemUsage save(RewardShopMemberItemUsage usage) {
            this.usage = usage;
            return usage;
        }

        private boolean hasUsage() {
            return usage != null;
        }

        private RewardShopMemberItemUsage usage() {
            return usage;
        }
    }

    private static class RecordingItemBalanceRepository implements RewardItemBalanceRepository {
        private final List<String> lockOrder;
        private RewardItemBalance balance;

        private RecordingItemBalanceRepository(List<String> lockOrder) {
            this.lockOrder = lockOrder;
        }

        @Override
        public Optional<RewardItemBalance> findByMemberIdAndShopItemId(Long memberId, Long shopItemId) {
            return Optional.ofNullable(balance);
        }

        @Override
        public Optional<RewardItemBalance> findByMemberIdAndShopItemIdForUpdate(Long memberId, Long shopItemId) {
            lockOrder.add("item-balance");
            return Optional.ofNullable(balance);
        }

        @Override
        public RewardItemBalance save(RewardItemBalance balance) {
            this.balance = balance;
            return balance;
        }

        private boolean hasBalance() {
            return balance != null;
        }

        private RewardItemBalance balance() {
            return balance;
        }
    }

    private static class RecordingPointRepository extends coin.coinzzickmock.testsupport.TestRewardPointRepository {
        private final List<String> lockOrder;
        private RewardPointWallet wallet = new RewardPointWallet(1L, 100);

        private RecordingPointRepository(List<String> lockOrder) {
            this.lockOrder = lockOrder;
        }

        @Override
        public Optional<RewardPointWallet> findByMemberId(Long memberId) {
            return Optional.of(wallet);
        }

        @Override
        public Optional<RewardPointWallet> findByMemberIdForUpdate(Long memberId) {
            lockOrder.add("wallet");
            return Optional.of(wallet);
        }

        @Override
        public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
            this.wallet = rewardPointWallet;
            return rewardPointWallet;
        }

        private RewardPointWallet wallet() {
            return wallet;
        }
    }

    private static class RecordingPointHistoryRepository extends coin.coinzzickmock.testsupport.TestRewardPointHistoryRepository {
        private final List<RewardPointHistory> histories = new ArrayList<>();

        @Override
        public RewardPointHistory save(RewardPointHistory history) {
            histories.add(history);
            return history;
        }

        private List<RewardPointHistory> histories() {
            return histories;
        }
    }

    private static RewardShopItem refillCountItem() {
        return new RewardShopItem(
                1L,
                REFILL_ITEM_CODE,
                "리필 횟수 추가권",
                "다음 KST 월요일 00:00 리셋 전까지 사용할 수 있는 지갑 리필 횟수 1회",
                RewardShopItem.ITEM_TYPE_ACCOUNT_REFILL_COUNT,
                20,
                true,
                null,
                0,
                null,
                20
        );
    }

    private static RewardShopItem positionPeekItem() {
        return new RewardShopItem(
                3L,
                POSITION_PEEK_ITEM_CODE,
                "포지션 엿보기권",
                "리더보드에서 선택한 사용자 1명의 현재 포지션 공개 요약을 1회 스냅샷으로 저장합니다.",
                RewardShopItem.ITEM_TYPE_POSITION_PEEK,
                30,
                true,
                null,
                0,
                null,
                30
        );
    }

    private static RewardShopItem coffeeVoucher() {
        return new RewardShopItem(
                2L,
                COFFEE_ITEM_CODE,
                "커피 교환권",
                "관리자가 발송하는 커피 교환권",
                RewardShopItem.ITEM_TYPE_COFFEE_VOUCHER,
                100,
                true,
                100,
                0,
                1,
                10
        );
    }
}
