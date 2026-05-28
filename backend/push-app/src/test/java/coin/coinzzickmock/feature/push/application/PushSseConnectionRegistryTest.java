package coin.coinzzickmock.feature.push.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PushSseConnectionRegistryTest {
    @Test
    void rejectsMissingKeysAsBadRequest() {
        PushSseConnectionRegistry registry = new PushSseConnectionRegistry(properties(10, 10));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registry.register(Set.of(), "tab-1", 30_000)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsTotalSubscriberOverflowAsTooManyRequests() {
        PushSseConnectionRegistry registry = new PushSseConnectionRegistry(properties(10, 1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registry.register(Set.of("summary:BTCUSDT", "candle:BTCUSDT:1m"), "tab-1", 30_000)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void rejectsPerKeySubscriberOverflowAsTooManyRequests() {
        PushSseConnectionRegistry registry = new PushSseConnectionRegistry(properties(1, 10));
        registry.register("summary:BTCUSDT", null, 30_000);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registry.register("summary:BTCUSDT", null, 30_000)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private PushServerProperties properties(int maxSubscribersPerKey, int maxTotalSubscribers) {
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
                maxSubscribersPerKey,
                maxTotalSubscribers
        );
    }
}
