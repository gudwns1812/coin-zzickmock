package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.query.GetWalletHistoryQuery;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.repository.WalletHistoryRepository;
import coin.coinzzickmock.feature.account.application.result.WalletHistoryResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetWalletHistoryService {
    private static final long DEFAULT_HISTORY_DAYS = 30;
    private static final String CURRENT_SNAPSHOT = "CURRENT_SNAPSHOT";

    private final WalletHistoryRepository walletHistoryRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<WalletHistoryResult> execute(GetWalletHistoryQuery query) {
        Instant now = Instant.now();
        Instant to = query.to() == null ? now : query.to();
        Instant from = query.from() == null ? to.minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS) : query.from();
        if (from.isAfter(to)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "지갑 이력 조회 기간을 확인해주세요.");
        }

        List<WalletHistoryResult> results = walletHistoryRepository
                .findByMemberIdBetween(query.memberId(), from, to).stream()
                .map(snapshot -> new WalletHistoryResult(
                        snapshot.walletBalance(),
                        snapshot.availableMargin(),
                        snapshot.sourceType(),
                        snapshot.sourceReference(),
                        snapshot.recordedAt()
                ))
                .toList();
        if (!results.isEmpty()) {
            return results;
        }

        TradingAccount account = accountRepository.findByMemberId(query.memberId())
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        return List.of(new WalletHistoryResult(
                account.walletBalance(),
                account.availableMargin(),
                CURRENT_SNAPSHOT,
                "account:" + account.memberId() + ":current",
                now
        ));
    }
}
