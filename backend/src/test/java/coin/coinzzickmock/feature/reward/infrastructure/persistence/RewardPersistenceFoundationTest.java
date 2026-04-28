package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberRole;
import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.grant.RewardPointGrantProcessor;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
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
    private static final String DEMO_MEMBER_ID = "test";

    @Autowired
    private RewardShopItemRepository rewardShopItemRepository;

    @Autowired
    private RewardPointRepository rewardPointRepository;

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
    void demoMemberSeedCreatesOrPromotesTestAccountAsAdmin() {
        assertEquals(
                MemberRole.ADMIN,
                memberCredentialRepository.findByMemberId(DEMO_MEMBER_ID).orElseThrow().role()
        );
    }

    @Test
    void grantUsesIntegerWalletAndWritesHistoryWithBalanceAfter() {
        rewardPointRepository.save(RewardPointWallet.empty(DEMO_MEMBER_ID));

        rewardPointGrantProcessor.grant(new GrantProfitPointCommand(DEMO_MEMBER_ID, 20_000));

        RewardPointWallet wallet = rewardPointRepository.findByMemberId(DEMO_MEMBER_ID).orElseThrow();
        assertEquals(10, wallet.rewardPoint());
        assertEquals(1, rewardPointHistoryEntityRepository.countByMemberId(DEMO_MEMBER_ID));
    }

    @Test
    void shopItemUsesSoldQuantityAndPurchaseLimitDerivedState() {
        RewardShopItem item = rewardShopItemRepository.findByCode("voucher.coffee").orElseThrow();

        assertNotNull(item.remainingPurchaseLimit(0));
        assertEquals(1, item.remainingPurchaseLimit(0));
        assertTrue(item.memberReachedLimit(1));
    }
}
