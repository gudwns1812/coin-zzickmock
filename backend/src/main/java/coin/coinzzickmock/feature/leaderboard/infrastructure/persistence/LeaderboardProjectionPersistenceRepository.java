package coin.coinzzickmock.feature.leaderboard.infrastructure.persistence;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.infrastructure.persistence.TradingAccountEntity;
import coin.coinzzickmock.feature.account.infrastructure.persistence.TradingAccountEntityRepository;
import coin.coinzzickmock.feature.leaderboard.application.port.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LeaderboardProjectionPersistenceRepository implements LeaderboardProjectionRepository {
    private final TradingAccountEntityRepository tradingAccountEntityRepository;

    @Override
    public List<LeaderboardEntry> findAll() {
        return tradingAccountEntityRepository.findAll().stream()
                .map(TradingAccountEntity::toDomain)
                .map(this::toEntry)
                .toList();
    }

    @Override
    public Optional<LeaderboardEntry> findByMemberId(String memberId) {
        return tradingAccountEntityRepository.findById(memberId)
                .map(TradingAccountEntity::toDomain)
                .map(this::toEntry);
    }

    private LeaderboardEntry toEntry(TradingAccount account) {
        double profitRate = (account.walletBalance() - TradingAccount.INITIAL_WALLET_BALANCE)
                / TradingAccount.INITIAL_WALLET_BALANCE;
        return new LeaderboardEntry(
                account.memberId(),
                account.memberName(),
                account.walletBalance(),
                profitRate,
                Instant.now()
        );
    }
}
