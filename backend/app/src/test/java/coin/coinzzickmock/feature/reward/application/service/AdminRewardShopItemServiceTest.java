package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardRedemptionRequestRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.result.AdminShopItemResult;
import coin.coinzzickmock.feature.reward.application.result.RewardRedemptionResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = CoinZzickmockApplication.class,
        properties = {
                "spring.task.scheduling.enabled=false",
                "spring.mail.host="
        }
)
@ActiveProfiles("test")
@Transactional
class AdminRewardShopItemServiceTest {
    private static final Long MEMBER_ID = 1L;
    private static final String ITEM_CODE = "voucher.coffee";

    @Autowired
    private AdminRewardShopItemService adminRewardShopItemService;

    @Autowired
    private CreateRewardRedemptionService createRewardRedemptionService;

    @Autowired
    private RewardPointRepository rewardPointRepository;

    @Autowired
    private RewardShopItemRepository rewardShopItemRepository;

    @Autowired
    private RewardRedemptionRequestRepository rewardRedemptionRequestRepository;

    @Test
    void listsInactiveItemsForAdmin() {
        adminRewardShopItemService.deactivate(ITEM_CODE);

        assertTrue(adminRewardShopItemService.list().stream()
                .anyMatch(item -> item.code().equals(ITEM_CODE) && !item.active()));
    }

    @Test
    void createsShopItem() {
        AdminShopItemResult result = adminRewardShopItemService.create(command("voucher.snack", 150, true, 5, 2));

        assertEquals("voucher.snack", result.code());
        assertEquals("스낵 교환권", result.name());
        assertEquals(150, result.price());
        assertEquals(5, result.totalStock());
        assertEquals(0, result.soldQuantity());
        assertEquals(2, result.perMemberPurchaseLimit());
    }

    @Test
    void rejectsDuplicateCode() {
        CoreException thrown = assertThrows(CoreException.class,
                () -> adminRewardShopItemService.create(command(ITEM_CODE, 100, true, 10, 1)));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
    }

    @Test
    void rejectsInvalidCreateInput() {
        CoreException thrown = assertThrows(CoreException.class,
                () -> adminRewardShopItemService.create(command("voucher.zero", 0, true, 10, 1)));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
    }

    @Test
    void updatesMutableFieldsWithoutChangingRedemptionSnapshot() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 200));
        RewardRedemptionResult created = createRewardRedemptionService.create(MEMBER_ID, ITEM_CODE, "010-1234-5678");

        AdminShopItemResult updated = adminRewardShopItemService.update(
                ITEM_CODE,
                command(null, "새 커피 교환권", "설명 변경", "COFFEE_VOUCHER", 120, true, 100, 1, 1)
        );

        assertEquals("새 커피 교환권", updated.name());
        assertEquals(120, updated.price());
        RewardRedemptionRequest request = rewardRedemptionRequestRepository.findByRequestIdForUpdate(created.requestId())
                .orElseThrow();
        assertEquals("커피 교환권", request.itemName());
        assertEquals(100, request.itemPrice());
    }

    @Test
    void rejectsTotalStockBelowSoldQuantity() {
        rewardPointRepository.save(new RewardPointWallet(MEMBER_ID, 200));
        createRewardRedemptionService.create(MEMBER_ID, ITEM_CODE, "010-1234-5678");

        CoreException thrown = assertThrows(CoreException.class,
                () -> adminRewardShopItemService.update(ITEM_CODE, command(null, 100, true, 0, 1)));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        RewardShopItem item = rewardShopItemRepository.findByCode(ITEM_CODE).orElseThrow();
        assertEquals(1, item.soldQuantity());
        assertEquals(100, item.totalStock());
    }

    @Test
    void deactivatesShopItem() {
        AdminShopItemResult result = adminRewardShopItemService.deactivate(ITEM_CODE);

        assertFalse(result.active());
        assertFalse(rewardShopItemRepository.findByCode(ITEM_CODE).orElseThrow().active());
    }

    private AdminRewardShopItemService.AdminShopItemCommand command(
            String code,
            int price,
            boolean active,
            Integer totalStock,
            Integer perMemberPurchaseLimit
    ) {
        return command(code, "스낵 교환권", "관리자가 발송하는 스낵 교환권", "SNACK_VOUCHER",
                price, active, totalStock, perMemberPurchaseLimit, 20);
    }

    private AdminRewardShopItemService.AdminShopItemCommand command(
            String code,
            String name,
            String description,
            String itemType,
            int price,
            boolean active,
            Integer totalStock,
            Integer perMemberPurchaseLimit,
            int sortOrder
    ) {
        return new AdminRewardShopItemService.AdminShopItemCommand(
                code,
                name,
                description,
                itemType,
                price,
                active,
                totalStock,
                perMemberPurchaseLimit,
                sortOrder
        );
    }
}
