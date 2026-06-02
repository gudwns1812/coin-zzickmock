package coin.coinzzickmock.feature.order.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ModifyOrderRequest(@NotNull @Positive BigDecimal limitPrice) {
}
