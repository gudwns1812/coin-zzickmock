package coin.coinzzickmock.feature.account.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.account.application.query.GetAccountSummaryQuery;
import coin.coinzzickmock.feature.account.application.result.AccountSummaryResult;
import coin.coinzzickmock.feature.account.application.service.GetAccountSummaryService;
import coin.coinzzickmock.providers.Providers;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/futures/account")
@RequiredArgsConstructor
public class AccountController {
    private final GetAccountSummaryService getAccountSummaryService;
    private final Providers providers;

    @GetMapping("/me")
    public ApiResponse<AccountSummaryResponse> me() {
        AccountSummaryResult result = getAccountSummaryService.execute(
                new GetAccountSummaryQuery(providers.auth().currentActor().memberId())
        );
        return ApiResponse.success(new AccountSummaryResponse(
                result.memberId(),
                result.memberName(),
                result.walletBalance(),
                result.availableMargin(),
                result.rewardPoint()
        ));
    }
}
