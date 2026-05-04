package coin.coinzzickmock.feature.leaderboard.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.leaderboard.application.service.GetLeaderboardService;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/futures/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {
    private final GetLeaderboardService getLeaderboardService;
    private final Providers providers;

    @GetMapping
    public ApiResponse<LeaderboardResponse> get(
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String limit
    ) {
        Long currentMemberId = providers.auth().currentActorOptional()
                .map(Actor::memberId)
                .orElse(null);
        return ApiResponse.success(LeaderboardResponse.from(getLeaderboardService.get(mode, limit, currentMemberId)));
    }
}
