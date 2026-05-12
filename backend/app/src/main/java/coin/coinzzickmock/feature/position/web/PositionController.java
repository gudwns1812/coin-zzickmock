package coin.coinzzickmock.feature.position.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.position.application.result.ClosePositionResult;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.application.service.ClosePositionService;
import coin.coinzzickmock.feature.position.application.service.GetOpenPositionsService;
import coin.coinzzickmock.feature.position.application.service.GetPositionHistoryService;
import coin.coinzzickmock.feature.position.application.service.UpdatePositionLeverageService;
import coin.coinzzickmock.feature.position.application.service.UpdatePositionTpslService;
import coin.coinzzickmock.providers.Providers;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/futures/positions")
@RequiredArgsConstructor
public class PositionController {
    private final GetOpenPositionsService getOpenPositionsService;
    private final GetPositionHistoryService getPositionHistoryService;
    private final ClosePositionService closePositionService;
    private final UpdatePositionTpslService updatePositionTpslService;
    private final UpdatePositionLeverageService updatePositionLeverageService;
    private final Providers providers;

    @GetMapping("/me")
    public ApiResponse<List<PositionSummaryResponse>> me() {
        List<PositionSummaryResponse> responses = getOpenPositionsService
                .getPositions(providers.auth().currentActor().memberId())
                .stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/history")
    public ApiResponse<List<PositionHistoryResponse>> history(@RequestParam(required = false) String symbol) {
        List<PositionHistoryResponse> responses = getPositionHistoryService
                .getHistory(providers.auth().currentActor().memberId(), symbol)
                .stream()
                .map(PositionHistoryResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping("/close")
    public ApiResponse<ClosePositionResponse> close(@RequestBody ClosePositionRequest request) {
        ClosePositionResult result = closePositionService.close(
                providers.auth().currentActor().memberId(),
                request.symbol(),
                request.positionSide(),
                request.marginMode(),
                request.quantity(),
                request.orderType(),
                request.limitPrice()
        );
        return ApiResponse.success(new ClosePositionResponse(
                result.symbol(),
                result.closedQuantity(),
                result.realizedPnl(),
                result.grantedPoint()
        ));
    }

    @PatchMapping("/tpsl")
    public ApiResponse<PositionSummaryResponse> updateTpsl(@RequestBody UpdatePositionTpslRequest request) {
        PositionSnapshotResult result = updatePositionTpslService.update(
                providers.auth().currentActor().memberId(),
                request.symbol(),
                request.positionSide(),
                request.marginMode(),
                request.takeProfitPrice(),
                request.stopLossPrice()
        );
        return ApiResponse.success(toResponse(result));
    }

    @PatchMapping("/leverage")
    public ApiResponse<PositionSummaryResponse> updateLeverage(@RequestBody UpdatePositionLeverageRequest request) {
        PositionSnapshotResult result = updatePositionLeverageService.update(
                providers.auth().currentActor().memberId(),
                request.symbol(),
                request.positionSide(),
                request.marginMode(),
                request.leverage()
        );
        return ApiResponse.success(toResponse(result));
    }

    private PositionSummaryResponse toResponse(PositionSnapshotResult result) {
        return new PositionSummaryResponse(
                result.symbol(),
                result.positionSide(),
                result.marginMode(),
                result.leverage(),
                result.quantity(),
                result.entryPrice(),
                result.markPrice(),
                result.liquidationPrice(),
                result.liquidationPriceType(),
                result.unrealizedPnl(),
                result.realizedPnl(),
                result.margin(),
                result.roi(),
                result.accumulatedClosedQuantity(),
                result.pendingCloseQuantity(),
                result.closeableQuantity(),
                result.takeProfitPrice(),
                result.stopLossPrice()
        );
    }
}
