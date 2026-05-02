package coin.coinzzickmock.feature.position.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.position.application.result.PositionHistoryResult;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PositionHistoryResponseTest {
    @Test
    void realizedPnlApiFieldIsMappedToNetPnl() {
        PositionHistoryResponse response = PositionHistoryResponse.from(new PositionHistoryResult(
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
                "MANUAL"
        ));

        assertEquals(1987.5, response.realizedPnl(), 0.0001);
        assertEquals(2000, response.grossRealizedPnl(), 0.0001);
        assertEquals(1987.5, response.netRealizedPnl(), 0.0001);
    }
}
