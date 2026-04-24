package coin.coinzzickmock.feature.order.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.result.CancelOrderResult;
import coin.coinzzickmock.feature.order.application.result.CreateOrderResult;
import coin.coinzzickmock.feature.order.application.result.OpenOrderResult;
import coin.coinzzickmock.feature.order.application.result.OrderHistoryResult;
import coin.coinzzickmock.feature.order.application.service.CancelOrderService;
import coin.coinzzickmock.feature.order.application.service.CreateOrderService;
import coin.coinzzickmock.feature.order.application.service.GetOpenOrdersService;
import coin.coinzzickmock.feature.order.application.service.GetOrderHistoryService;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.providers.Providers;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/futures/orders")
public class OrderController {
    private final CreateOrderService createOrderService;
    private final GetOpenOrdersService getOpenOrdersService;
    private final GetOrderHistoryService getOrderHistoryService;
    private final CancelOrderService cancelOrderService;
    private final TradingExecutionSseBroker tradingExecutionSseBroker;
    private final Providers providers;
    private final long streamTimeoutMs;

    public OrderController(
            CreateOrderService createOrderService,
            GetOpenOrdersService getOpenOrdersService,
            GetOrderHistoryService getOrderHistoryService,
            CancelOrderService cancelOrderService,
            TradingExecutionSseBroker tradingExecutionSseBroker,
            Providers providers,
            @Value("${coin.trading.sse.timeout-ms:300000}") long streamTimeoutMs
    ) {
        this.createOrderService = createOrderService;
        this.getOpenOrdersService = getOpenOrdersService;
        this.getOrderHistoryService = getOrderHistoryService;
        this.cancelOrderService = cancelOrderService;
        this.tradingExecutionSseBroker = tradingExecutionSseBroker;
        this.providers = providers;
        this.streamTimeoutMs = streamTimeoutMs;
    }

    @GetMapping("/open")
    public ApiResponse<List<OpenOrderResponse>> open(@RequestParam(required = false) String symbol) {
        List<OpenOrderResult> orders = getOpenOrdersService.getOpenOrders(
                providers.auth().currentActor().memberId(),
                symbol
        );
        return ApiResponse.success(orders.stream().map(OpenOrderResponse::from).toList());
    }

    @GetMapping("/history")
    public ApiResponse<List<OrderHistoryResponse>> history(@RequestParam(required = false) String symbol) {
        List<OrderHistoryResult> orders = getOrderHistoryService.getOrderHistory(
                providers.auth().currentActor().memberId(),
                symbol
        );
        return ApiResponse.success(orders.stream().map(OrderHistoryResponse::from).toList());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        String memberId = providers.auth().currentActor().memberId();
        TradingExecutionSseBroker.SseSubscriptionPermit permit = tradingExecutionSseBroker.reserve(memberId);
        SseEmitter emitter = createEmitter();
        try {
            tradingExecutionSseBroker.register(permit, emitter);
            return emitter;
        } catch (RuntimeException exception) {
            tradingExecutionSseBroker.release(permit);
            throw exception;
        }
    }

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

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<CancelOrderResponse> cancel(@PathVariable String orderId) {
        CancelOrderResult result = cancelOrderService.cancel(
                providers.auth().currentActor().memberId(),
                orderId
        );
        return ApiResponse.success(CancelOrderResponse.from(result));
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

    SseEmitter createEmitter() {
        return new SseEmitter(streamTimeoutMs);
    }
}
