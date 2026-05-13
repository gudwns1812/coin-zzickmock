package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.repository.WalletHistoryRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistoryDate;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SnapshotWalletHistoryService {
    private final AccountRepository accountRepository;
    private final WalletHistoryRepository walletHistoryRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordChangedBalance(WalletBalanceChangedEvent event) {
        if (event.hasAccountSnapshot()) {
            TradingAccount account = new TradingAccount(
                    event.memberId(),
                    "",
                    "",
                    event.walletBalance(),
                    0,
                    event.accountVersion()
            );
            walletHistoryRepository.updateCurrentDay(account, WalletHistoryDate.from(event.observedAt()));
            return;
        }
        recordObservedBalance(event.memberId(), event.observedAt());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCurrentDay(Long memberId) {
        recordObservedBalance(memberId, Instant.now(clock));
    }

    private void recordObservedBalance(Long memberId, Instant observedAt) {
        TradingAccount account = accountRepository.findByMemberId(memberId).orElse(null);
        if (account == null) {
            return;
        }
        walletHistoryRepository.updateCurrentDay(account, WalletHistoryDate.from(observedAt));
    }

    @Transactional
    public void createTodayBaselines() {
        LocalDate today = today();
        accountRepository.findAll()
                .forEach(account -> walletHistoryRepository.createBaselineIfAbsent(account, today));
    }

    private LocalDate today() {
        return WalletHistoryDate.from(Instant.now(clock));
    }
}
