package coin.coinzzickmock.feature.push.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.feature.push.application.PushServerProperties;
import coin.coinzzickmock.feature.push.application.PushSseConnectionRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PushStreamControllerTest {
    @Test
    void rejectsBlankMarketSummarySymbolsAsBadRequest() {
        PushStreamController controller = new PushStreamController(
                new PushSseConnectionRegistry(properties()),
                properties(),
                null
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.marketSummaryStream(" , ", "tab-1")
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private PushServerProperties properties() {
        return new PushServerProperties(
                true,
                "test-push-app",
                "coin:push:market:v1",
                "coin:push:trading:v1",
                "push-server-market-v1",
                "push-server-trading-v1",
                100,
                50,
                Duration.ofSeconds(15),
                Duration.ofSeconds(5),
                300_000,
                100,
                1000
        );
    }
}
