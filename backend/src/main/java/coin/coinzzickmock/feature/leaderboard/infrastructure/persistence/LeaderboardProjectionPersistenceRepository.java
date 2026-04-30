package coin.coinzzickmock.feature.leaderboard.infrastructure.persistence;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.infrastructure.persistence.TradingAccountEntity;
import coin.coinzzickmock.feature.account.infrastructure.persistence.TradingAccountEntityRepository;
import coin.coinzzickmock.feature.leaderboard.application.port.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LeaderboardProjectionPersistenceRepository implements LeaderboardProjectionRepository {
    private final TradingAccountEntityRepository tradingAccountEntityRepository;
    private final MemberCredentialRepository memberCredentialRepository;

    @Override
    public List<LeaderboardEntry> findAll() {
        return tradingAccountEntityRepository.findAll().stream()
                .map(TradingAccountEntity::toDomain)
                .map(account -> toEntry(account, nicknameOf(account)))
                .toList();
    }

    @Override
    public Optional<LeaderboardEntry> findByMemberId(Long memberId) {
        return tradingAccountEntityRepository.findById(memberId)
                .map(TradingAccountEntity::toDomain)
                .map(account -> toEntry(account, nicknameOf(account)));
    }

    private String nicknameOf(TradingAccount account) {
        return memberCredentialRepository.findByMemberId(account.memberId())
                .map(member -> member.nickname())
                .orElse(account.memberName());
    }

    private LeaderboardEntry toEntry(TradingAccount account, String nickname) {
        double profitRate = (account.walletBalance() - TradingAccount.INITIAL_WALLET_BALANCE)
                / TradingAccount.INITIAL_WALLET_BALANCE;
        return new LeaderboardEntry(
                account.memberId(),
                nickname,
                account.walletBalance(),
                profitRate,
                Instant.now()
        );
    }
}
