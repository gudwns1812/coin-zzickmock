package coin.coinzzickmock.feature.member.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
class GoogleOAuthPendingTokenCodec {
    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    PendingToken issue() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new PendingToken(rawToken, hash(rawToken));
    }

    String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new coin.coinzzickmock.common.error.CoreException(
                    coin.coinzzickmock.common.error.ErrorCode.OAUTH_ONBOARDING_EXPIRED);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available.", exception);
        }
    }

    record PendingToken(String rawToken, String tokenHash) {
    }
}
