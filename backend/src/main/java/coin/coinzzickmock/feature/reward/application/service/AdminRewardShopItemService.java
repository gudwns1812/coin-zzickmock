package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.result.AdminShopItemResult;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRewardShopItemService {
    private final RewardShopItemRepository rewardShopItemRepository;

    @Transactional(readOnly = true)
    public List<AdminShopItemResult> list() {
        return rewardShopItemRepository.findAllItems().stream()
                .map(AdminShopItemResult::from)
                .toList();
    }

    @Transactional
    public AdminShopItemResult create(AdminShopItemCommand command) {
        AdminShopItemCommand normalized = normalize(command);
        String code = requireText(normalized.code(), String::trim);
        rewardShopItemRepository.findByCode(code)
                .ifPresent(existing -> {
                    throw invalid();
                });
        RewardShopItem item = new RewardShopItem(
                null,
                code,
                normalized.name(),
                normalized.description(),
                normalized.itemType(),
                normalized.price(),
                normalized.active(),
                normalized.totalStock(),
                0,
                normalized.perMemberPurchaseLimit(),
                normalized.sortOrder()
        );
        try {
            return AdminShopItemResult.from(rewardShopItemRepository.save(item));
        } catch (DataIntegrityViolationException exception) {
            log.warn("Reward shop item create failed because of a data integrity violation. operation=admin_reward_shop_item_create",
                    exception);
            throw invalid();
        }
    }

    @Transactional
    public AdminShopItemResult update(String code, AdminShopItemCommand command) {
        AdminShopItemCommand normalized = normalize(command);
        RewardShopItem current = findForUpdate(code);
        if (normalized.totalStock() != null && normalized.totalStock() < current.soldQuantity()) {
            throw invalid();
        }
        RewardShopItem item = new RewardShopItem(
                current.id(),
                current.code(),
                normalized.name(),
                normalized.description(),
                normalized.itemType(),
                normalized.price(),
                normalized.active(),
                normalized.totalStock(),
                current.soldQuantity(),
                normalized.perMemberPurchaseLimit(),
                normalized.sortOrder()
        );
        return AdminShopItemResult.from(rewardShopItemRepository.save(item));
    }

    @Transactional
    public AdminShopItemResult deactivate(String code) {
        RewardShopItem current = findForUpdate(code);
        RewardShopItem item = new RewardShopItem(
                current.id(),
                current.code(),
                current.name(),
                current.description(),
                current.itemType(),
                current.price(),
                false,
                current.totalStock(),
                current.soldQuantity(),
                current.perMemberPurchaseLimit(),
                current.sortOrder()
        );
        return AdminShopItemResult.from(rewardShopItemRepository.save(item));
    }

    private RewardShopItem findForUpdate(String code) {
        String normalizedCode = requireText(code, String::trim);
        return rewardShopItemRepository.findByCodeForUpdate(normalizedCode)
                .orElseThrow(() -> invalid());
    }

    private AdminShopItemCommand normalize(AdminShopItemCommand command) {
        if (command == null) {
            throw invalid();
        }
        String code = command.code() == null ? null : command.code().trim();
        String name = requireText(command.name(), String::trim);
        String description = requireText(command.description(), String::trim);
        String itemType = requireText(command.itemType(), String::trim);
        if (command.price() <= 0) {
            throw invalid();
        }
        if (command.totalStock() != null && command.totalStock() < 0) {
            throw invalid();
        }
        if (command.perMemberPurchaseLimit() != null && command.perMemberPurchaseLimit() <= 0) {
            throw invalid();
        }
        return new AdminShopItemCommand(
                code,
                name,
                description,
                itemType,
                command.price(),
                command.active(),
                command.totalStock(),
                command.perMemberPurchaseLimit(),
                command.sortOrder()
        );
    }

    private String requireText(String value, Function<String, String> normalizer) {
        if (value == null || value.isBlank()) {
            throw invalid();
        }
        return normalizer.apply(value);
    }

    private CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }

    public record AdminShopItemCommand(
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
    }
}
