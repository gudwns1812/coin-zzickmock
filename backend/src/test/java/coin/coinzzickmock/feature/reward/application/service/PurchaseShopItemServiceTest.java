package coin.coinzzickmock.feature.reward.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.feature.account.application.refill.AccountRefillCreditProcessor;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository.LockedAccountRefillState;
import coin.coinzzickmock.feature.account.application.service.AccountRefillDatePolicy;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopMemberItemUsageRepository;
import coin.coinzzickmock.feature.reward.application.result.ShopPurchaseResult;
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
    @Test
    void locksRefillStateBeforeRewardRowsToAvoidAccountRewardRefillDeadlocks() {
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
                new AccountRefillCreditProcessor(refillStateRepository, new AccountRefillDatePolicy())
        );

        ShopPurchaseResult result = service.purchase(1L, "account.refill-count");

        assertEquals(80, result.rewardPoint());
        assertEquals(2, result.refillRemainingCount());
        assertTrue(lockOrder.contains("refill-state"), "refill-state lock should be acquired");
        assertTrue(lockOrder.contains("shop-item"), "shop-item lock should be acquired");
        assertTrue(lockOrder.contains("usage"), "usage lock should be acquired");
        assertTrue(lockOrder.contains("wallet"), "wallet lock should be acquired");
        assertTrue(lockOrder.indexOf("refill-state") < lockOrder.indexOf("shop-item"));
        assertTrue(lockOrder.indexOf("refill-state") < lockOrder.indexOf("usage"));
        assertTrue(lockOrder.indexOf("refill-state") < lockOrder.indexOf("wallet"));
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
        public void provisionDailyStateIfAbsent(Long memberId, LocalDate refillDate) {
            lockOrder.add("refill-state");
            if (state == null) {
                state = AccountRefillState.daily(memberId, refillDate);
            }
        }

        @Override
        public AccountRefillState grantExtraRefillCount(Long memberId, LocalDate refillDate, int count) {
            lockOrder.add("refill-state");
            if (state == null) {
                state = AccountRefillState.daily(memberId, refillDate);
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
    }

    private static class RecordingShopItemRepository implements RewardShopItemRepository {
        private final List<String> lockOrder;
        private RewardShopItem item = new RewardShopItem(
                1L,
                "account.refill-count",
                "리필 횟수 추가권",
                "오늘 자정 전까지 사용할 수 있는 지갑 리필 횟수 1회",
                RewardShopItem.ITEM_TYPE_ACCOUNT_REFILL_COUNT,
                20,
                true,
                null,
                0,
                null,
                20
        );

        private RecordingShopItemRepository(List<String> lockOrder) {
            this.lockOrder = lockOrder;
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
    }
}
