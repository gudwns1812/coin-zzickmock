package coin.coinzzickmock.feature.positionpeek.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.positionpeek.application.dto.PositionPeekTargetTokenPayload;
import coin.coinzzickmock.feature.positionpeek.application.repository.PositionPeekTargetTokenStore;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PositionPeekTargetTokenRegistryTest {
    @Test
    void issuesOpaqueTokenAndResolvesPayloadFromStore() {
        InMemoryTargetTokenStore store = new InMemoryTargetTokenStore();
        PositionPeekTargetTokenRegistry registry = new PositionPeekTargetTokenRegistry(store);
        PositionPeekTargetTokenPayload payload = payload();

        String targetToken = registry.issue(payload);

        assertThat(targetToken).isNotBlank();
        assertThat(targetToken).isNotEqualTo(payload.targetMemberId().toString());
        assertThat(store.savedTtl).isEqualTo(Duration.ofMinutes(10));
        assertThat(registry.validate(targetToken)).isEqualTo(payload);
    }

    @Test
    void rejectsUnknownToken() {
        PositionPeekTargetTokenRegistry registry = new PositionPeekTargetTokenRegistry(
                new InMemoryTargetTokenStore()
        );

        assertThrows(CoreException.class, () -> registry.validate("missing-token"));
    }

    private PositionPeekTargetTokenPayload payload() {
        return new PositionPeekTargetTokenPayload(
                7L,
                2,
                "target",
                120_000.0,
                20.0,
                "profitRate"
        );
    }

    private static class InMemoryTargetTokenStore implements PositionPeekTargetTokenStore {
        private final Map<String, PositionPeekTargetTokenPayload> payloads = new HashMap<>();
        private Duration savedTtl;

        @Override
        public void save(String tokenHash, PositionPeekTargetTokenPayload payload, Duration ttl) {
            payloads.put(tokenHash, payload);
            savedTtl = ttl;
        }

        @Override
        public Optional<PositionPeekTargetTokenPayload> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(payloads.get(tokenHash));
        }
    }
}
