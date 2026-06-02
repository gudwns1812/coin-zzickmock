package coin.coinzzickmock.common.web;

import static org.junit.jupiter.api.Assertions.assertFalse;

import coin.coinzzickmock.feature.order.web.CreateOrderRequest;
import coin.coinzzickmock.feature.position.web.ClosePositionRequest;
import coin.coinzzickmock.feature.reward.web.AdminShopItemRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class WebRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsUnsupportedCreateOrderTypeAtWebRequestBoundary() {
        CreateOrderRequest request = new CreateOrderRequest(
                "BTCUSDT",
                "LONG",
                "LIMT",
                "ISOLATED",
                10,
                0.1,
                75_000.0
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsUnsupportedCloseOrderTypeAtWebRequestBoundary() {
        ClosePositionRequest request = new ClosePositionRequest(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                0.1,
                "STOP",
                101_000.0
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidAdminShopItemShapeAtWebRequestBoundary() {
        AdminShopItemRequest request = new AdminShopItemRequest(
                "voucher.zero",
                "커피 교환권",
                "설명",
                "COFFEE_VOUCHER",
                0,
                true,
                10,
                1,
                1
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMissingAdminShopItemCodeOnlyForCreateRequest() {
        AdminShopItemRequest request = new AdminShopItemRequest(
                null,
                "커피 교환권",
                "설명",
                "COFFEE_VOUCHER",
                100,
                true,
                10,
                1,
                1
        );

        assertFalse(validator.validate(request, AdminShopItemRequest.Create.class).isEmpty());
    }
}
