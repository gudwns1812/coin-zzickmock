package coin.coinzzickmock.feature.leaderboard.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.leaderboard.application.service.GetLeaderboardService;
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

    @GetMapping
    public ApiResponse<LeaderboardResponse> get(
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String limit
    ) {
        return ApiResponse.success(LeaderboardResponse.from(getLeaderboardService.get(mode, limit)));
    }
}
