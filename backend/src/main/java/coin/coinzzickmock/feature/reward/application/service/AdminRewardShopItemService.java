package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.application.result.AdminShopItemResult;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

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
        String code = requireText(normalized.code(), "상점 상품 코드는 필수입니다.", String::trim);
        rewardShopItemRepository.findByCode(code)
                .ifPresent(existing -> {
                    throw invalid("이미 존재하는 상점 상품 코드입니다.");
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
            throw invalid("이미 존재하는 상점 상품 코드입니다.");
        }
    }

    @Transactional
    public AdminShopItemResult update(String code, AdminShopItemCommand command) {
        AdminShopItemCommand normalized = normalize(command);
        RewardShopItem current = findForUpdate(code);
        if (normalized.totalStock() != null && normalized.totalStock() < current.soldQuantity()) {
            throw invalid("총 재고는 이미 판매/예약된 수량보다 작을 수 없습니다.");
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
        String normalizedCode = requireText(code, "상점 상품 코드는 필수입니다.", String::trim);
        return rewardShopItemRepository.findByCodeForUpdate(normalizedCode)
                .orElseThrow(() -> invalid("존재하지 않는 상점 상품입니다."));
    }

    private AdminShopItemCommand normalize(AdminShopItemCommand command) {
        if (command == null) {
            throw invalid("상점 상품 정보는 필수입니다.");
        }
        String code = command.code() == null ? null : command.code().trim();
        String name = requireText(command.name(), "상점 상품명은 필수입니다.", String::trim);
        String description = requireText(command.description(), "상점 상품 설명은 필수입니다.", String::trim);
        String itemType = requireText(command.itemType(), "상점 상품 타입은 필수입니다.", String::trim);
        if (command.price() <= 0) {
            throw invalid("상점 상품 가격은 0보다 커야 합니다.");
        }
        if (command.totalStock() != null && command.totalStock() < 0) {
            throw invalid("총 재고는 음수일 수 없습니다.");
        }
        if (command.perMemberPurchaseLimit() != null && command.perMemberPurchaseLimit() <= 0) {
            throw invalid("회원별 구매 제한은 0보다 커야 합니다.");
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

    private String requireText(String value, String message, Function<String, String> normalizer) {
        if (value == null || value.isBlank()) {
            throw invalid(message);
        }
        return normalizer.apply(value);
    }

    private CoreException invalid(String message) {
        return new CoreException(ErrorCode.INVALID_REQUEST, message);
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
