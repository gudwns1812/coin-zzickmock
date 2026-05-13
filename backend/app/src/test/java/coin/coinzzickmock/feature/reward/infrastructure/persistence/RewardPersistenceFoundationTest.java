package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberRole;
import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.grant.RewardPointGrantProcessor;
import coin.coinzzickmock.feature.reward.application.repository.RewardItemBalanceRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardRedemptionRequestRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.domain.RewardItemBalance;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.domain.RewardPhoneNumber;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = CoinZzickmockApplication.class,
        properties = "spring.task.scheduling.enabled=false"
)
@ActiveProfiles("test")
@Transactional
class RewardPersistenceFoundationTest {
    private static final String DEMO_ACCOUNT = "test";

    @Autowired
    private RewardShopItemRepository rewardShopItemRepository;

    @Autowired
    private RewardPointRepository rewardPointRepository;

    @Autowired
    private RewardItemBalanceRepository rewardItemBalanceRepository;

    @Autowired
    private RewardRedemptionRequestRepository rewardRedemptionRequestRepository;

    @Autowired
    private RewardPointGrantProcessor rewardPointGrantProcessor;

    @Autowired
    private RewardPointHistoryEntityRepository rewardPointHistoryEntityRepository;

    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Test
    void seedsCoffeeVoucherAsActiveDbShopItem() {
        RewardShopItem item = rewardShopItemRepository.findByCode("voucher.coffee").orElseThrow();

        assertEquals("커피 교환권", item.name());
        assertEquals(100, item.price());
        assertTrue(item.active());
        assertEquals(100, item.totalStock());
        assertEquals(0, item.soldQuantity());
        assertEquals(100, item.remainingStock());
        assertEquals(1, item.perMemberPurchaseLimit());
        assertFalse(item.soldOut());
        assertTrue(rewardShopItemRepository.findActiveItems().stream()
                .anyMatch(activeItem -> activeItem.code().equals("voucher.coffee")));
    }


    @Test
    void seedsPositionPeekAsActiveConsumableShopItem() {
        RewardShopItem item = rewardShopItemRepository.findByCode("position.peek").orElseThrow();

        assertEquals("포지션 엿보기권", item.name());
        assertEquals(RewardShopItem.ITEM_TYPE_POSITION_PEEK, item.itemType());
        assertEquals(30, item.price());
        assertTrue(item.active());
        assertTrue(item.positionPeek());
        assertTrue(item.instantConsumable());
        assertTrue(rewardShopItemRepository.findActiveItems().stream()
                .anyMatch(activeItem -> activeItem.code().equals("position.peek")));
    }

    @Test
    void rewardItemBalancePersistsNonNegativeRemainingQuantity() {
        Long memberId = demoMemberId();
        RewardShopItem item = rewardShopItemRepository.findByCode("position.peek").orElseThrow();

        RewardItemBalance saved = rewardItemBalanceRepository.save(
                RewardItemBalance.empty(memberId, item.id()).addOne()
        );

        RewardItemBalance locked = rewardItemBalanceRepository
                .findByMemberIdAndShopItemIdForUpdate(memberId, item.id())
                .orElseThrow();
        assertEquals(saved.remainingQuantity(), locked.remainingQuantity());
        assertEquals(1, locked.remainingQuantity());
        RewardItemBalance consumed = rewardItemBalanceRepository.save(locked.consumeOne());
        assertEquals(0, consumed.remainingQuantity());
    }

    @Test
    void demoMemberSeedCreatesOrPromotesTestAccountAsAdmin() {
        assertEquals(
                MemberRole.ADMIN,
                memberCredentialRepository.findActiveByAccount(DEMO_ACCOUNT).orElseThrow().role()
        );
    }

    @Test
    void grantUsesIntegerWalletAndWritesHistoryWithBalanceAfter() {
        Long demoMemberId = demoMemberId();
        rewardPointRepository.save(RewardPointWallet.empty(demoMemberId));

        rewardPointGrantProcessor.grant(new GrantProfitPointCommand(demoMemberId, 20_000));

        RewardPointWallet wallet = rewardPointRepository.findByMemberId(demoMemberId).orElseThrow();
        assertEquals(10, wallet.rewardPoint());
        assertEquals(1, rewardPointHistoryEntityRepository.countByMemberId(demoMemberId));
    }

    @Test
    void shopItemUsesSoldQuantityAndPurchaseLimitDerivedState() {
        RewardShopItem item = rewardShopItemRepository.findByCode("voucher.coffee").orElseThrow();

        assertNotNull(item.remainingPurchaseLimit(0));
        assertEquals(1, item.remainingPurchaseLimit(0));
        assertTrue(item.memberReachedLimit(1));
    }

    @Test
    void redemptionApprovalClaimUsesManagedEntityUpdateOnce() {
        RewardRedemptionRequest request = savePendingRedemption();
        Instant approvedAt = Instant.parse("2026-05-05T00:00:00Z");

        Long adminMemberId = demoMemberId();
        int claimed = rewardRedemptionRequestRepository.claimPendingAsApproved(
                request.requestId(),
                adminMemberId,
                "sent",
                approvedAt
        );
        int duplicateClaim = rewardRedemptionRequestRepository.claimPendingAsApproved(
                request.requestId(),
                adminMemberId,
                "sent again",
                approvedAt.plusSeconds(1)
        );

        RewardRedemptionRequest persisted = rewardRedemptionRequestRepository
                .findByRequestId(request.requestId())
                .orElseThrow();
        assertEquals(1, claimed);
        assertEquals(0, duplicateClaim);
        assertEquals(RewardRedemptionStatus.APPROVED, persisted.status());
        assertEquals(adminMemberId, persisted.adminMemberId());
        assertEquals("sent", persisted.adminMemo());
        assertEquals(approvedAt, persisted.sentAt());
    }

    @Test
    void redemptionRejectionClaimUsesManagedEntityUpdateOnce() {
        RewardRedemptionRequest request = savePendingRedemption();
        Instant rejectedAt = Instant.parse("2026-05-05T00:00:00Z");

        Long adminMemberId = demoMemberId();
        int claimed = rewardRedemptionRequestRepository.claimPendingAsRejected(
                request.requestId(),
                adminMemberId,
                "refunded",
                rejectedAt
        );
        int duplicateClaim = rewardRedemptionRequestRepository.claimPendingAsRejected(
                request.requestId(),
                adminMemberId,
                "refunded again",
                rejectedAt.plusSeconds(1)
        );

        RewardRedemptionRequest persisted = rewardRedemptionRequestRepository
                .findByRequestId(request.requestId())
                .orElseThrow();
        assertEquals(1, claimed);
        assertEquals(0, duplicateClaim);
        assertEquals(RewardRedemptionStatus.REJECTED, persisted.status());
        assertEquals(adminMemberId, persisted.adminMemberId());
        assertEquals("refunded", persisted.adminMemo());
        assertEquals(rejectedAt, persisted.cancelledAt());
    }

    @Test
    void redemptionCancelClaimChecksOwnerAndUsesManagedEntityUpdateOnce() {
        RewardRedemptionRequest request = savePendingRedemption();
        Instant cancelledAt = Instant.parse("2026-05-05T00:00:00Z");

        int wrongOwnerClaim = rewardRedemptionRequestRepository.claimPendingAsCancelled(
                request.requestId(),
                request.memberId() + 1,
                cancelledAt
        );
        int claimed = rewardRedemptionRequestRepository.claimPendingAsCancelled(
                request.requestId(),
                request.memberId(),
                cancelledAt
        );
        int duplicateClaim = rewardRedemptionRequestRepository.claimPendingAsCancelled(
                request.requestId(),
                request.memberId(),
                cancelledAt.plusSeconds(1)
        );

        RewardRedemptionRequest persisted = rewardRedemptionRequestRepository
                .findByRequestId(request.requestId())
                .orElseThrow();
        assertEquals(0, wrongOwnerClaim);
        assertEquals(1, claimed);
        assertEquals(0, duplicateClaim);
        assertEquals(RewardRedemptionStatus.CANCELLED, persisted.status());
        assertEquals(cancelledAt, persisted.cancelledAt());
    }

    private Long demoMemberId() {
        return memberCredentialRepository.findActiveByAccount(DEMO_ACCOUNT).orElseThrow().memberId();
    }

    private RewardRedemptionRequest savePendingRedemption() {
        RewardShopItem item = rewardShopItemRepository.findByCode("voucher.coffee").orElseThrow();
        return rewardRedemptionRequestRepository.save(RewardRedemptionRequest.pending(
                "request-" + UUID.randomUUID(),
                demoMemberId(),
                item,
                RewardPhoneNumber.from("010-1234-5678"),
                Instant.parse("2026-05-05T00:00:00Z")
        ));
    }
}
