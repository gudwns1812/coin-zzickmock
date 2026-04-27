package coin.coinzzickmock.feature.position.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.position.application.repository.PositionHistoryRepository;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
class PositionPersistenceRepositoryTest {
    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PositionHistoryRepository positionHistoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void savesAndLoadsOpenPositionThroughH2() {
        String memberId = "position-owner-" + UUID.randomUUID();
        saveAccount(memberId);

        positionRepository.save(memberId, new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.15,
                100000,
                100250,
                90000.0,
                37.5
        ));

        PositionSnapshot loaded = positionRepository.findOpenPosition(memberId, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();

        assertEquals(0.15, loaded.quantity(), 0.000001);
        assertEquals(0, loaded.accumulatedOpenFee(), 0.000001);
        assertEquals(0, loaded.accumulatedFundingCost(), 0.000001);
        assertEquals(0, loaded.version());
        assertEquals(1, positionRepository.findOpenPositions(memberId).size());
    }

    @Test
    void updatesOpenPositionOnlyWhenVersionMatchesAndIncrementsVersion() {
        String memberId = "position-owner-" + UUID.randomUUID();
        saveAccount(memberId);
        positionRepository.save(memberId, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000
        ));
        PositionSnapshot loaded = positionRepository.findOpenPosition(memberId, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();

        PositionMutationResult result = positionRepository.updateWithVersion(
                memberId,
                loaded,
                loaded.close(0.1, 101000, 101000, 0.0005).remainingPosition()
        );

        assertEquals(PositionMutationResult.Status.UPDATED, result.status());
        assertEquals(1, result.affectedRows());
        assertEquals(1, result.updatedSnapshot().version());
        PositionSnapshot persisted = positionRepository.findOpenPosition(memberId, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(0.1, persisted.quantity(), 0.000001);
        assertEquals(1, persisted.version());
    }

    @Test
    void guardedMutationReportsStaleVersionWithoutOverwritingCurrentPosition() {
        String memberId = "position-owner-" + UUID.randomUUID();
        saveAccount(memberId);
        positionRepository.save(memberId, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000
        ));
        PositionSnapshot stale = positionRepository.findOpenPosition(memberId, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        positionRepository.updateWithVersion(
                memberId,
                stale,
                stale.close(0.1, 101000, 101000, 0.0005).remainingPosition()
        );

        PositionMutationResult result = positionRepository.deleteWithVersion(memberId, stale);

        assertEquals(PositionMutationResult.Status.STALE_VERSION, result.status());
        assertEquals(0, result.affectedRows());
        assertEquals(1, result.updatedSnapshot().version());
        PositionSnapshot persisted = positionRepository.findOpenPosition(memberId, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(0.1, persisted.quantity(), 0.000001);
        assertEquals(1, persisted.version());
    }

    @Test
    void findsOpenPositionCandidatesWithOwnerIdentityAndDeletesOnce() {
        String memberId = "position-owner-" + UUID.randomUUID();
        saveAccount(memberId);
        positionRepository.save(memberId, new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "CROSS",
                10,
                0.2,
                100000,
                99500,
                90000.0,
                -100
        ));

        List<OpenPositionCandidate> candidates = positionRepository.findOpenBySymbol("BTCUSDT");

        assertTrue(candidates.stream().anyMatch(candidate -> candidate.memberId().equals(memberId)));
        assertTrue(positionRepository.deleteIfOpen(memberId, "BTCUSDT", "LONG", "CROSS"));
        assertFalse(positionRepository.deleteIfOpen(memberId, "BTCUSDT", "LONG", "CROSS"));
        assertTrue(positionRepository.findOpenPosition(memberId, "BTCUSDT", "LONG", "CROSS").isEmpty());
    }

    @Test
    void savesAndLoadsPositionHistoryNetPnlAccountingFieldsThroughH2() {
        String memberId = "position-owner-" + UUID.randomUUID();
        saveAccount(memberId);
        PositionHistory saved = positionHistoryRepository.save(memberId, new PositionHistory(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                Instant.parse("2026-04-27T00:00:00Z"),
                100000,
                110000,
                0.2,
                1987.5,
                2000,
                1.5,
                11,
                12.5,
                0,
                1987.5,
                0.99375,
                Instant.parse("2026-04-27T01:00:00Z"),
                PositionHistory.CLOSE_REASON_MANUAL
        ));

        PositionHistory loaded = positionHistoryRepository.findByMemberId(memberId, "BTCUSDT").get(0);

        assertEquals(saved.realizedPnl(), loaded.realizedPnl(), 0.0001);
        assertEquals(2000, loaded.grossRealizedPnl(), 0.0001);
        assertEquals(1.5, loaded.openFee(), 0.0001);
        assertEquals(11, loaded.closeFee(), 0.0001);
        assertEquals(12.5, loaded.totalFee(), 0.0001);
        assertEquals(0, loaded.fundingCost(), 0.0001);
        assertEquals(1987.5, loaded.netRealizedPnl(), 0.0001);
    }

    private void saveAccount(String memberId) {
        accountRepository.save(new TradingAccount(
                memberId,
                memberId + "@coinzzickmock.dev",
                memberId,
                100000,
                100000
        ));
    }
}
