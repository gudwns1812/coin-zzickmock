package coin.coinzzickmock.feature.leaderboard.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.leaderboard.application.service.GetLeaderboardService;
import coin.coinzzickmock.feature.positionpeek.application.service.SearchLeaderboardMembersService;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/futures/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {
    private static final String MODE_PATTERN = "(?i)^(profitRate|walletBalance)$";

    private final GetLeaderboardService getLeaderboardService;
    private final SearchLeaderboardMembersService searchLeaderboardMembersService;
    private final Providers providers;

    @GetMapping
    public ApiResponse<LeaderboardResponse> get(
            @Pattern(regexp = MODE_PATTERN) @RequestParam(defaultValue = "profitRate") String mode,
            @Min(1) @Max(50) @RequestParam(defaultValue = "5") int limit
    ) {
        Long currentMemberId = providers.auth().currentActorOptional()
                .map(Actor::memberId)
                .orElse(null);
        return ApiResponse.success(LeaderboardResponse.from(getLeaderboardService.get(parseMode(mode), limit, currentMemberId)));
    }

    @GetMapping("/search")
    public ApiResponse<List<LeaderboardEntryResponse>> search(
            @Pattern(regexp = MODE_PATTERN) @RequestParam(defaultValue = "profitRate") String mode,
            @NotBlank @RequestParam String query,
            @Min(1) @Max(20) @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(searchLeaderboardMembersService.search(parseMode(mode), query.trim().toLowerCase(java.util.Locale.ROOT), limit).stream()
                .map(LeaderboardEntryResponse::from)
                .toList());
    }

    private coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode parseMode(String mode) {
        return coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode.parse(mode)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
    }
}
