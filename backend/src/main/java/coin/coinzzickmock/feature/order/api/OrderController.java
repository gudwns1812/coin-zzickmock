package coin.coinzzickmock.feature.order.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.result.CreateOrderResult;
import coin.coinzzickmock.feature.order.application.service.CreateOrderService;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.providers.Providers;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/futures/orders")
public class OrderController {
    private final CreateOrderService createOrderService;
    private final Providers providers;

    public OrderController(CreateOrderService createOrderService, Providers providers) {
        this.createOrderService = createOrderService;
        this.providers = providers;
    }

    @PostMapping("/preview")
    public ApiResponse<OrderPreviewResponse> preview(@RequestBody CreateOrderRequest request) {
        OrderPreview preview = createOrderService.preview(toCommand(request));
        boolean executable = "TAKER".equalsIgnoreCase(preview.feeType()) || "MARKET".equalsIgnoreCase(request.orderType());
        return ApiResponse.success(new OrderPreviewResponse(
                preview.feeType(),
                preview.estimatedFee(),
                preview.estimatedInitialMargin(),
                preview.estimatedLiquidationPrice(),
                request.limitPrice() != null ? request.limitPrice() : preview.estimatedInitialMargin() * request.leverage() / request.quantity(),
                executable
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
