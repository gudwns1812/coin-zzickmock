package coin.coinzzickmock.feature.leaderboard.infrastructure.persistence;

import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LeaderboardProjectionPersistenceRepository implements LeaderboardProjectionRepository {
    private static final String ACTIVE_LEADERBOARD_SQL = """
            SELECT account.member_id,
                   member.nickname,
                   account.wallet_balance
              FROM trading_accounts account
              JOIN member_credentials member
                ON member.id = account.member_id
             WHERE member.withdrawn_at IS NULL
            """;
    private static final String LEADERBOARD_ORDER = " ORDER BY account.wallet_balance DESC, account.member_id ASC";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<LeaderboardEntry> rowMapper = (resultSet, rowNumber) ->
            LeaderboardEntry.fromWalletBalance(
                    resultSet.getLong("member_id"),
                    resultSet.getString("nickname"),
                    resultSet.getBigDecimal("wallet_balance"),
                    Instant.now()
            );

    @Override
    public List<LeaderboardEntry> findAll() {
        return jdbcTemplate.query(ACTIVE_LEADERBOARD_SQL + LEADERBOARD_ORDER, rowMapper);
    }

    @Override
    public Optional<LeaderboardEntry> findByMemberId(Long memberId) {
        return jdbcTemplate.query(
                        ACTIVE_LEADERBOARD_SQL + " AND account.member_id = ?" + LEADERBOARD_ORDER,
                        rowMapper,
                        memberId
                ).stream()
                .findFirst();
    }
}
