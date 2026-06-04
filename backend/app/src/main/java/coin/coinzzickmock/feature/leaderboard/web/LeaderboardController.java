package coin.coinzzickmock.feature.leaderboard.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.leaderboard.application.service.GetLeaderboardService;
import coin.coinzzickmock.feature.positionpeek.application.service.SearchLeaderboardMembersService;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import java.util.List;
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
    private final SearchLeaderboardMembersService searchLeaderboardMembersService;
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

    @GetMapping("/search")
    public ApiResponse<List<LeaderboardEntryResponse>> search(
            @RequestParam(required = false) String mode,
            @RequestParam String query,
            @RequestParam(required = false) String limit
    ) {
        return ApiResponse.success(searchLeaderboardMembersService.search(mode, query, limit).stream()
                .map(LeaderboardEntryResponse::from)
                .toList());
    }
}
