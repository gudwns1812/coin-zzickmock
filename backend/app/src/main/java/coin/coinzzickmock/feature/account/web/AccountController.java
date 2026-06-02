package coin.coinzzickmock.feature.account.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.account.application.query.GetAccountSummaryQuery;
import coin.coinzzickmock.feature.account.application.query.GetWalletHistoryQuery;
import coin.coinzzickmock.feature.account.application.dto.AccountSummaryResult;
import coin.coinzzickmock.feature.account.application.dto.AccountRefillResult;
import coin.coinzzickmock.feature.account.application.dto.AccountRefillStatusResult;
import coin.coinzzickmock.feature.account.application.service.GetAccountSummaryService;
import coin.coinzzickmock.feature.account.application.service.GetAccountRefillStatusService;
import coin.coinzzickmock.feature.account.application.service.GetWalletHistoryService;
import coin.coinzzickmock.feature.account.application.service.RefillTradingAccountService;
import coin.coinzzickmock.providers.Providers;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/futures/account")
@RequiredArgsConstructor
public class AccountController {
    private final GetAccountSummaryService getAccountSummaryService;
    private final GetAccountRefillStatusService getAccountRefillStatusService;
    private final RefillTradingAccountService refillTradingAccountService;
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

    @GetMapping("/me/refill")
    public ApiResponse<AccountRefillStatusResponse> refillStatus() {
        AccountRefillStatusResult result = getAccountRefillStatusService.get(providers.auth().currentActor().memberId());
        return ApiResponse.success(new AccountRefillStatusResponse(
                result.remainingCount(),
                result.refillable(),
                result.disabledReason(),
                result.targetWalletBalance(),
                result.targetAvailableMargin(),
                result.nextResetAt()
        ));
    }

    @PostMapping("/me/refill")
    public ApiResponse<AccountRefillResponse> refill() {
        AccountRefillResult result = refillTradingAccountService.refill(providers.auth().currentActor().memberId());
        return ApiResponse.success(new AccountRefillResponse(
                result.walletBalance(),
                result.availableMargin(),
                result.remainingCount()
        ));
    }

    @GetMapping("/me/wallet-history")
    public ApiResponse<List<WalletHistoryResponse>> walletHistory(@Valid WalletHistoryRequest request) {
        List<WalletHistoryResponse> responses = getWalletHistoryService.execute(
                        new GetWalletHistoryQuery(providers.auth().currentActor().memberId(), request.from(), request.to())
                ).stream()
                .map(WalletHistoryResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }
}
