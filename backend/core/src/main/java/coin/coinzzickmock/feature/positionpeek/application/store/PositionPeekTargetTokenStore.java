package coin.coinzzickmock.feature.positionpeek.application.store;

import coin.coinzzickmock.feature.positionpeek.application.dto.PositionPeekTargetTokenPayload;
import java.time.Duration;
import java.util.Optional;

public interface PositionPeekTargetTokenStore {
    void save(String tokenHash, PositionPeekTargetTokenPayload payload, Duration ttl);

    Optional<PositionPeekTargetTokenPayload> findByTokenHash(String tokenHash);
}
