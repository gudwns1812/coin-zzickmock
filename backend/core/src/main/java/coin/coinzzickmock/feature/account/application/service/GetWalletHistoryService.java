package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.query.GetWalletHistoryQuery;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.repository.WalletHistoryRepository;
import coin.coinzzickmock.feature.account.application.result.WalletHistoryResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistoryDate;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetWalletHistoryService {
    private static final long DEFAULT_HISTORY_DAYS = 30;

    private final WalletHistoryRepository walletHistoryRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<WalletHistoryResult> execute(GetWalletHistoryQuery query) {
        Instant now = Instant.now();
        LocalDate today = WalletHistoryDate.from(now);
        LocalDate to = query.to() == null ? today : WalletHistoryDate.from(query.to());
        LocalDate from = query.from() == null ? to.minusDays(DEFAULT_HISTORY_DAYS - 1) : WalletHistoryDate.from(query.from());
        if (from.isAfter(to)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        List<WalletHistoryResult> results = new ArrayList<>(walletHistoryRepository
                .findByMemberIdBetween(query.memberId(), from, to).stream()
                .map(GetWalletHistoryService::toResult)
                .toList());
        if (!from.isAfter(today)
                && !to.isBefore(today)
                && results.stream().noneMatch(result -> result.snapshotDate().equals(today))) {
            results.add(toResult(currentAccountSnapshot(query.memberId(), today, now)));
        }
        if (!results.isEmpty()) {
            return results;
        }

        return List.of();
    }

    private WalletHistorySnapshot currentAccountSnapshot(Long memberId, LocalDate snapshotDate, Instant now) {
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        BigDecimal baselineWalletBalance = walletHistoryRepository.findLatestBefore(memberId, snapshotDate)
                .map(WalletHistorySnapshot::walletBalance)
                .orElse(BigDecimal.valueOf(account.walletBalance()));
        BigDecimal walletBalance = BigDecimal.valueOf(account.walletBalance());
        return new WalletHistorySnapshot(
                memberId,
                snapshotDate,
                baselineWalletBalance,
                walletBalance,
                walletBalance.subtract(baselineWalletBalance),
                now
        );
    }

    private static WalletHistoryResult toResult(WalletHistorySnapshot snapshot) {
        return new WalletHistoryResult(
                snapshot.snapshotDate(),
                snapshot.walletBalance(),
                snapshot.dailyWalletChange(),
                snapshot.recordedAt()
        );
    }
}
