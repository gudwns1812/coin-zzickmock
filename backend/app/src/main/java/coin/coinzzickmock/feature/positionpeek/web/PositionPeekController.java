package coin.coinzzickmock.feature.positionpeek.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.positionpeek.application.service.PositionPeekService;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/futures/position-peeks")
@RequiredArgsConstructor
public class PositionPeekController {
    private final PositionPeekService positionPeekService;
    private final Providers providers;

    @PostMapping
    public ApiResponse<PositionPeekSnapshotResponse> consume(@Valid @RequestBody PositionPeekRequest request) {
        Actor actor = providers.auth().currentActor();
        return ApiResponse.success(PositionPeekSnapshotResponse.from(
                positionPeekService.consume(actor.memberId(), request.targetToken())
                        .latestSnapshot()
        ));
    }

    @PostMapping("/latest")
    public ApiResponse<PositionPeekStatusResponse> latest(@Valid @RequestBody PositionPeekRequest request) {
        Actor actor = providers.auth().currentActor();
        return ApiResponse.success(PositionPeekStatusResponse.from(
                positionPeekService.latest(actor.memberId(), request.targetToken())
        ));
    }

    @GetMapping("/{peekId}")
    public ApiResponse<PositionPeekSnapshotResponse> get(@NotBlank @PathVariable String peekId) {
        Actor actor = providers.auth().currentActor();
        return ApiResponse.success(PositionPeekSnapshotResponse.from(
                positionPeekService.getSnapshot(actor.memberId(), peekId)
        ));
    }
}
