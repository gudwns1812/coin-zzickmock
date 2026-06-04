package coin.coinzzickmock.feature.positionpeek.application.token;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.positionpeek.application.dto.PositionPeekTargetTokenPayload;
import coin.coinzzickmock.feature.positionpeek.application.store.PositionPeekTargetTokenStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class PositionPeekTargetTokenRegistry {
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);
    private static final int TOKEN_BYTES = 32;

    private final PositionPeekTargetTokenStore targetTokenStore;
    private final SecureRandom secureRandom = new SecureRandom();

    public PositionPeekTargetTokenRegistry(PositionPeekTargetTokenStore targetTokenStore) {
        this.targetTokenStore = targetTokenStore;
    }

    public String issue(PositionPeekTargetTokenPayload payload) {
        if (payload == null || payload.targetMemberId() == null) {
            throw invalid();
        }
        String targetToken = randomToken();
        targetTokenStore.save(fingerprint(targetToken), payload, TOKEN_TTL);
        return targetToken;
    }

    public PositionPeekTargetTokenPayload validate(String targetToken) {
        if (targetToken == null || targetToken.isBlank()) {
            throw invalid();
        }
        return targetTokenStore.findByTokenHash(fingerprint(targetToken))
                .orElseThrow(this::invalid);
    }

    public String fingerprint(String targetToken) {
        if (targetToken == null || targetToken.isBlank()) {
            throw invalid();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(targetToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
