package coin.coinzzickmock.feature.order.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.result.CreateOrderResult;
import coin.coinzzickmock.feature.order.application.service.CreateOrderService;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.providers.Providers;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/futures/orders")
@RequiredArgsConstructor
public class OrderController {
    private final CreateOrderService createOrderService;
    private final Providers providers;

    @PostMapping("/preview")
    public ApiResponse<OrderPreviewResponse> preview(@RequestBody CreateOrderRequest request) {
        OrderPreview preview = createOrderService.preview(toCommand(request));
        return ApiResponse.success(new OrderPreviewResponse(
                preview.feeType(),
                preview.estimatedFee(),
                preview.estimatedInitialMargin(),
                preview.estimatedLiquidationPrice(),
                preview.estimatedEntryPrice(),
                preview.executable()
        ));
    }

    @PostMapping
    public ApiResponse<OrderExecutionResponse> create(@RequestBody CreateOrderRequest request) {
        CreateOrderResult result = createOrderService.execute(toCommand(request));
        return ApiResponse.success(new OrderExecutionResponse(
                result.orderId(),
                result.status(),
                result.symbol(),
                result.feeType(),
                result.estimatedFee(),
                result.estimatedInitialMargin(),
                result.estimatedLiquidationPrice(),
                result.executionPrice()
        ));
    }

    private CreateOrderCommand toCommand(CreateOrderRequest request) {
        return new CreateOrderCommand(
                providers.auth().currentActor().memberId(),
                request.symbol(),
                request.positionSide(),
                request.orderType(),
                request.marginMode(),
                request.leverage(),
                request.quantity(),
                request.limitPrice()
        );
    }
}
