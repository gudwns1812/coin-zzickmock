package coin.coinzzickmock.feature.position.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.position.application.result.ClosePositionResult;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.application.service.ClosePositionService;
import coin.coinzzickmock.feature.position.application.service.GetOpenPositionsService;
import coin.coinzzickmock.providers.Providers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/futures/positions")
public class PositionController {
    private final GetOpenPositionsService getOpenPositionsService;
    private final ClosePositionService closePositionService;
    private final Providers providers;

    public PositionController(
            GetOpenPositionsService getOpenPositionsService,
            ClosePositionService closePositionService,
            Providers providers
    ) {
        this.getOpenPositionsService = getOpenPositionsService;
        this.closePositionService = closePositionService;
        this.providers = providers;
    }

    @GetMapping("/me")
    public ApiResponse<List<PositionSummaryResponse>> me() {
        List<PositionSummaryResponse> responses = getOpenPositionsService
                .getPositions(providers.auth().currentActor().memberId())
                .stream()
                .map(this::toResponse)
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
                request.quantity()
        );
        return ApiResponse.success(new ClosePositionResponse(
                result.symbol(),
                result.closedQuantity(),
                result.realizedPnl(),
                result.grantedPoint()
        ));
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
                result.unrealizedPnl()
        );
    }
}
