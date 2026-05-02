package coin.coinzzickmock.feature.account.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.account.application.query.GetAccountSummaryQuery;
import coin.coinzzickmock.feature.account.application.query.GetWalletHistoryQuery;
import coin.coinzzickmock.feature.account.application.result.AccountSummaryResult;
import coin.coinzzickmock.feature.account.application.service.GetAccountSummaryService;
import coin.coinzzickmock.feature.account.application.service.GetWalletHistoryService;
import coin.coinzzickmock.providers.Providers;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/futures/account")
@RequiredArgsConstructor
public class AccountController {
    private final GetAccountSummaryService getAccountSummaryService;
    private final GetWalletHistoryService getWalletHistoryService;
    private final Providers providers;

    @GetMapping("/me")
    public ApiResponse<AccountSummaryResponse> me() {
        AccountSummaryResult result = getAccountSummaryService.execute(
                new GetAccountSummaryQuery(providers.auth().currentActor().memberId())
        );
        return ApiResponse.success(new AccountSummaryResponse(
                result.memberId(),
                result.account(),
                result.memberName(),
                result.nickname(),
                result.usdtBalance(),
                result.walletBalance(),
                result.available(),
                result.totalUnrealizedPnl(),
                result.roi(),
                result.rewardPoint()
        ));
    }

    @GetMapping("/me/wallet-history")
    public ApiResponse<List<WalletHistoryResponse>> walletHistory(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        List<WalletHistoryResponse> responses = getWalletHistoryService.execute(
                        new GetWalletHistoryQuery(providers.auth().currentActor().memberId(), from, to)
                ).stream()
                .map(WalletHistoryResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }
}
