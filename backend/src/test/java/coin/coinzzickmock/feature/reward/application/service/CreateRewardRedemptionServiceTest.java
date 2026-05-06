package coin.coinzzickmock.feature.reward.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.reward.application.event.RewardRedemptionCreatedEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class CreateRewardRedemptionServiceTest {
    private static final Long MEMBER_ID = 1L;
    private static final String COFFEE_ITEM_CODE = "voucher.coffee";
    private static final String REFILL_ITEM_CODE = "account.refill-count";

    @Test
    void successfulRedemptionDeductsWalletPersistsRequestHistoryAndPublishesEventAfterSave() {
        List<String> operations = new ArrayList<>();
        InMemoryShopItemRepository shopItemRepository = new InMemoryShopItemRepository(operations, coffeeVoucher());
        InMemoryUsageRepository usageRepository = new InMemoryUsageRepository(operations);
        InMemoryPointRepository pointRepository = new InMemoryPointRepository(operations, 200);
        RecordingPointHistoryRepository historyRepository = new RecordingPointHistoryRepository(operations);
        InMemoryRedemptionRequestRepository redemptionRequestRepository =
                new InMemoryRedemptionRequestRepository(operations);
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher(operations);
        CreateRewardRedemptionService service = service(
                shopItemRepository,
                usageRepository,
                pointRepository,
                historyRepository,
                redemptionRequestRepository,
                eventPublisher
        );

        RewardRedemptionResult result = service.create(MEMBER_ID, COFFEE_ITEM_CODE, "010-1234-5678");

        assertEquals(RewardRedemptionStatus.PENDING, result.status());
        assertEquals(100, pointRepository.wallet().rewardPoint());
        assertEquals(1, shopItemRepository.item().soldQuantity());
        assertEquals(1, usageRepository.usage().purchaseCount());
        assertEquals(result.requestId(), redemptionRequestRepository.request().requestId());
        RewardPointHistory history = historyRepository.histories().get(0);
        assertEquals(-100, history.amount());
        assertEquals(100, history.balanceAfter());
        assertEquals("REDEMPTION_REQUEST", history.sourceType());
        assertEquals(result.requestId(), history.sourceReference());
        RewardRedemptionCreatedEvent event =
                assertInstanceOf(RewardRedemptionCreatedEvent.class, eventPublisher.events().get(0));
        assertEquals(result.requestId(), event.requestId());
        assertEquals(COFFEE_ITEM_CODE, event.itemCode());
        assertEquals(List.of("item", "usage", "wallet", "request", "history", "event"), operations);
    }

    @Test
    void invalidItemDoesNotMutateWalletUsageRequestHistoryOrPublishEvent() {
        List<String> operations = new ArrayList<>();
        InMemoryShopItemRepository shopItemRepository = new InMemoryShopItemRepository(operations, refillCountItem());
        InMemoryUsageRepository usageRepository = new InMemoryUsageRepository(operations);
        InMemoryPointRepository pointRepository = new InMemoryPointRepository(operations, 200);
        RecordingPointHistoryRepository historyRepository = new RecordingPointHistoryRepository(operations);
        InMemoryRedemptionRequestRepository redemptionRequestRepository =
                new InMemoryRedemptionRequestRepository(operations);
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher(operations);
        CreateRewardRedemptionService service = service(
                shopItemRepository,
                usageRepository,
                pointRepository,
                historyRepository,
                redemptionRequestRepository,
                eventPublisher
        );

        CoreException thrown = assertThrows(CoreException.class,
                () -> service.create(MEMBER_ID, REFILL_ITEM_CODE, "010-1234-5678"));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        assertEquals(200, pointRepository.wallet().rewardPoint());
        assertEquals(0, shopItemRepository.item().soldQuantity());
        assertFalse(usageRepository.hasUsage());
        assertFalse(redemptionRequestRepository.hasRequest());
        assertEquals(0, historyRepository.histories().size());
        assertEquals(0, eventPublisher.events().size());
        assertEquals(List.of(), operations);
    }

    private CreateRewardRedemptionService service(
            InMemoryShopItemRepository shopItemRepository,
            InMemoryUsageRepository usageRepository,
            InMemoryPointRepository pointRepository,
            RecordingPointHistoryRepository historyRepository,
            InMemoryRedemptionRequestRepository redemptionRequestRepository,
            CapturingEventPublisher eventPublisher
    ) {
        return new CreateRewardRedemptionService(
                shopItemRepository,
                usageRepository,
                pointRepository,
                historyRepository,
                redemptionRequestRepository,
                new AfterCommitEventPublisher(eventPublisher)
        );
    }

    private static class InMemoryShopItemRepository implements RewardShopItemRepository {
        private final List<String> operations;
        private RewardShopItem item;

        private InMemoryShopItemRepository(List<String> operations, RewardShopItem item) {
            this.operations = operations;
            this.item = item;
        }

        @Override
        public List<RewardShopItem> findAllItems() {
            return List.of(item);
        }

        @Override
        public List<RewardShopItem> findActiveItems() {
            return item.active() ? List.of(item) : List.of();
        }

        @Override
        public Optional<RewardShopItem> findByCode(String code) {
            return item.code().equals(code)
                    ? Optional.of(item)
                    : Optional.empty();
        }

        @Override
        public Optional<RewardShopItem> findByCodeForUpdate(String code) {
            return findByCode(code);
        }

        @Override
        public Optional<RewardShopItem> findByIdForUpdate(Long id) {
            return item.id().equals(id) ? Optional.of(item) : Optional.empty();
        }

        @Override
        public RewardShopItem save(RewardShopItem item) {
            operations.add("item");
            this.item = item;
            return item;
        }

        private RewardShopItem item() {
            return item;
        }
    }

    private static class InMemoryUsageRepository implements RewardShopMemberItemUsageRepository {
        private final List<String> operations;
        private RewardShopMemberItemUsage usage;

        private InMemoryUsageRepository(List<String> operations) {
            this.operations = operations;
        }

        @Override
        public Optional<RewardShopMemberItemUsage> findByMemberIdAndShopItemId(Long memberId, Long shopItemId) {
            return Optional.ofNullable(usage);
        }

        @Override
        public Optional<RewardShopMemberItemUsage> findByMemberIdAndShopItemIdForUpdate(
                Long memberId,
                Long shopItemId
        ) {
            return Optional.ofNullable(usage);
        }

        @Override
        public RewardShopMemberItemUsage save(RewardShopMemberItemUsage usage) {
            operations.add("usage");
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

    private static class InMemoryPointRepository extends coin.coinzzickmock.testsupport.TestRewardPointRepository {
        private final List<String> operations;
        private RewardPointWallet wallet;

        private InMemoryPointRepository(List<String> operations, int rewardPoint) {
            this.operations = operations;
            this.wallet = new RewardPointWallet(MEMBER_ID, rewardPoint);
        }

        @Override
        public Optional<RewardPointWallet> findByMemberId(Long memberId) {
            return Optional.of(wallet);
        }

        @Override
        public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
            operations.add("wallet");
            this.wallet = rewardPointWallet;
            return rewardPointWallet;
        }

        private RewardPointWallet wallet() {
            return wallet;
        }
    }

    private static class RecordingPointHistoryRepository extends coin.coinzzickmock.testsupport.TestRewardPointHistoryRepository {
        private final List<String> operations;
        private final List<RewardPointHistory> histories = new ArrayList<>();

        private RecordingPointHistoryRepository(List<String> operations) {
            this.operations = operations;
        }

        @Override
        public RewardPointHistory save(RewardPointHistory history) {
            operations.add("history");
            histories.add(history);
            return history;
        }

        private List<RewardPointHistory> histories() {
            return histories;
        }
    }

    private static class InMemoryRedemptionRequestRepository implements RewardRedemptionRequestRepository {
        private final List<String> operations;
        private RewardRedemptionRequest request;

        private InMemoryRedemptionRequestRepository(List<String> operations) {
            this.operations = operations;
        }

        @Override
        public RewardRedemptionRequest save(RewardRedemptionRequest request) {
            operations.add("request");
            this.request = request;
            return request;
        }

        @Override
        public Optional<RewardRedemptionRequest> findByRequestId(String requestId) {
            return Optional.ofNullable(request)
                    .filter(existing -> existing.requestId().equals(requestId));
        }

        @Override
        public Optional<RewardRedemptionRequest> findByRequestIdForUpdate(String requestId) {
            return findByRequestId(requestId);
        }

        @Override
        public List<RewardRedemptionRequest> findByMemberId(Long memberId) {
            return Optional.ofNullable(request)
                    .filter(existing -> existing.memberId().equals(memberId))
                    .map(List::of)
                    .orElseGet(List::of);
        }

        @Override
        public List<RewardRedemptionRequest> findByStatus(RewardRedemptionStatus status) {
            return Optional.ofNullable(request)
                    .filter(existing -> existing.status() == status)
                    .map(List::of)
                    .orElseGet(List::of);
        }

        @Override
        public int claimPendingAsApproved(String requestId, Long adminMemberId, String adminMemo, java.time.Instant approvedAt) {
            throw new UnsupportedOperationException("claimPendingAsApproved is not used in this test");
        }

        @Override
        public int claimPendingAsRejected(String requestId, Long adminMemberId, String adminMemo, java.time.Instant rejectedAt) {
            throw new UnsupportedOperationException("claimPendingAsRejected is not used in this test");
        }

        @Override
        public int claimPendingAsCancelled(String requestId, Long memberId, java.time.Instant cancelledAt) {
            throw new UnsupportedOperationException("claimPendingAsCancelled is not used in this test");
        }

        private boolean hasRequest() {
            return request != null;
        }

        private RewardRedemptionRequest request() {
            return request;
        }
    }

    private static class CapturingEventPublisher implements ApplicationEventPublisher {
        private final List<String> operations;
        private final List<Object> events = new ArrayList<>();

        private CapturingEventPublisher(List<String> operations) {
            this.operations = operations;
        }

        @Override
        public void publishEvent(Object event) {
            operations.add("event");
            events.add(event);
        }

        private List<Object> events() {
            return events;
        }
    }

    private static RewardShopItem coffeeVoucher() {
        return new RewardShopItem(
                1L,
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

    private static RewardShopItem refillCountItem() {
        return new RewardShopItem(
                2L,
                REFILL_ITEM_CODE,
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
    }
}
