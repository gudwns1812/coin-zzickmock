package coin.coinzzickmock.feature.reward.application.grant;

import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistoryType;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardPointGrantProcessorTest {
    @Test
    void doesNotPersistWalletWhenGrantedPointIsZero() {
        InMemoryRewardPointRepository repository = new InMemoryRewardPointRepository(new RewardPointWallet(1L, 7));
        InMemoryRewardPointHistoryRepository historyRepository = new InMemoryRewardPointHistoryRepository();
        RewardPointGrantProcessor processor = new RewardPointGrantProcessor(
                new RewardPointPolicy(),
                repository,
                historyRepository
        );

        RewardPointResult result = processor.grant(new GrantProfitPointCommand(1L, -5));

        assertEquals(7, result.rewardPoint());
        assertEquals("NONE", result.tierLabel());
        assertEquals(1, repository.lockingFindCount);
        assertEquals(0, repository.saveCount);
        assertEquals(0, historyRepository.histories.size());
    }

    @Test
    void writesGrantHistoryWithBalanceAfterWhenPointsAreGranted() {
        InMemoryRewardPointRepository repository = new InMemoryRewardPointRepository(new RewardPointWallet(1L, 7));
        InMemoryRewardPointHistoryRepository historyRepository = new InMemoryRewardPointHistoryRepository();
        RewardPointGrantProcessor processor = new RewardPointGrantProcessor(
                new RewardPointPolicy(),
                repository,
                historyRepository
        );

        RewardPointResult result = processor.grant(new GrantProfitPointCommand(1L, 20_000));

        assertEquals(17, result.rewardPoint());
        assertEquals(1, repository.lockingFindCount);
        assertEquals(1, repository.saveCount);
        assertEquals(1, historyRepository.histories.size());
        RewardPointHistory history = historyRepository.histories.get(0);
        assertEquals(RewardPointHistoryType.GRANT, history.historyType());
        assertEquals(10, history.amount());
        assertEquals(17, history.balanceAfter());
    }

    private static class InMemoryRewardPointRepository extends coin.coinzzickmock.testsupport.TestRewardPointRepository {
        private RewardPointWallet wallet;
        private int lockingFindCount;
        private int saveCount;

        private InMemoryRewardPointRepository(RewardPointWallet wallet) {
            this.wallet = wallet;
        }

        @Override
        public Optional<RewardPointWallet> findByMemberId(Long memberId) {
            return Optional.ofNullable(wallet).filter(current -> current.memberId().equals(memberId));
        }

        @Override
        public Optional<RewardPointWallet> findByMemberIdForUpdate(Long memberId) {
            lockingFindCount++;
            return findByMemberId(memberId);
        }

        @Override
        public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
            saveCount++;
            wallet = rewardPointWallet;
            return rewardPointWallet;
        }
    }

    private static class InMemoryRewardPointHistoryRepository extends coin.coinzzickmock.testsupport.TestRewardPointHistoryRepository {
        private final List<RewardPointHistory> histories = new ArrayList<>();

        @Override
        public RewardPointHistory save(RewardPointHistory history) {
            histories.add(history);
            return history;
        }
    }
}
