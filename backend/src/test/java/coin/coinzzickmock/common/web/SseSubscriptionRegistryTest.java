package coin.coinzzickmock.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseSubscriptionRegistryTest {
    @Test
    void replacesSameClientKeyWithoutConsumingExtraPermitWhenLimitsAreFull() {
        SseSubscriptionRegistry<String> registry = new SseSubscriptionRegistry<>(1, 1);
        SseEmitter first = new SseEmitter(0L);
        SseEmitter second = new SseEmitter(0L);

        registry.register(registry.reserve("BTCUSDT", "tab-1").permit(), first);
        SseSubscriptionRegistry.Registration<String> registration = registry.register(
                registry.reserve("BTCUSDT", "tab-1").permit(),
                second
        );

        assertThat(registration.registered()).isTrue();
        assertThat(registration.replacedEmitter()).isSameAs(first);
        assertThat(registry.subscribers("BTCUSDT")).containsExactly(second);
    }

    @Test
    void rejectsDistinctClientKeyWhenKeyLimitIsFull() {
        SseSubscriptionRegistry<String> registry = new SseSubscriptionRegistry<>(1, 2);

        registry.register(registry.reserve("BTCUSDT", "tab-1").permit(), new SseEmitter(0L));

        SseSubscriptionRegistry.Reservation<String> reservation = registry.reserve("BTCUSDT", "tab-2");

        assertThat(reservation.accepted()).isFalse();
        assertThat(reservation.rejection()).isEqualTo(SseSubscriptionRegistry.ReservationRejection.KEY_LIMIT);
    }

    @Test
    void oldEmitterCannotUnregisterReplacement() {
        SseSubscriptionRegistry<String> registry = new SseSubscriptionRegistry<>(1, 1);
        SseEmitter first = new SseEmitter(0L);
        SseEmitter second = new SseEmitter(0L);

        registry.register(registry.reserve("BTCUSDT", "tab-1").permit(), first);
        registry.register(registry.reserve("BTCUSDT", "tab-1").permit(), second);

        assertThat(registry.unregister("BTCUSDT", "tab-1", first)).isFalse();
        assertThat(registry.subscribers("BTCUSDT")).containsExactly(second);
    }

    @Test
    void removingLastClientKeyCleansSubscriberLimit() {
        SseSubscriptionRegistry<String> registry = new SseSubscriptionRegistry<>(1, 1);
        SseEmitter emitter = new SseEmitter(0L);

        registry.register(registry.reserve("BTCUSDT", "tab-1").permit(), emitter);

        assertThat(registry.unregister("BTCUSDT", "tab-1", emitter)).isTrue();
        assertThat(registry.hasSubscriberLimit("BTCUSDT")).isFalse();
    }
}
