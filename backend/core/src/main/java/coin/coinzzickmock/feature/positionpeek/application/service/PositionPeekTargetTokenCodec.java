package coin.coinzzickmock.feature.positionpeek.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PositionPeekTargetTokenCodec {
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] SIGNING_KEY = "coin-zzickmock-position-peek-target-v1".getBytes(StandardCharsets.UTF_8);
    private final Clock clock;

    public PositionPeekTargetTokenCodec() {
        this(Clock.systemUTC());
    }

    PositionPeekTargetTokenCodec(Clock clock) {
        this.clock = clock;
    }

    public String issue(TargetTokenPayload payload) {
        Instant issuedAt = Instant.now(clock);
        String body = String.join("|",
                String.valueOf(payload.targetMemberId()),
                String.valueOf(payload.rank() == null ? "" : payload.rank()),
                nullToEmpty(payload.nickname()),
                String.valueOf(payload.walletBalance() == null ? "" : payload.walletBalance()),
                String.valueOf(payload.profitRate() == null ? "" : payload.profitRate()),
                nullToEmpty(payload.leaderboardMode()),
                String.valueOf(issuedAt.toEpochMilli())
        );
        String signature = signature(body);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((body + "|" + signature).getBytes(StandardCharsets.UTF_8));
    }

    public TargetTokenPayload validate(String targetToken) {
        if (targetToken == null || targetToken.isBlank()) {
            throw invalid();
        }
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(targetToken), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
        String[] parts = decoded.split("\\|", -1);
        if (parts.length != 8) {
            throw invalid();
        }
        String body = String.join("|", parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
        if (!MessageDigest.isEqual(signature(body).getBytes(StandardCharsets.UTF_8), parts[7].getBytes(StandardCharsets.UTF_8))) {
            throw invalid();
        }
        Instant issuedAt;
        try {
            issuedAt = Instant.ofEpochMilli(Long.parseLong(parts[6]));
        } catch (NumberFormatException exception) {
            throw invalid();
        }
        if (issuedAt.plus(TOKEN_TTL).isBefore(Instant.now(clock))) {
            throw invalid();
        }
        try {
            return new TargetTokenPayload(
                    Long.parseLong(parts[0]),
                    parts[1].isBlank() ? null : Integer.parseInt(parts[1]),
                    parts[2],
                    parts[3].isBlank() ? null : Double.parseDouble(parts[3]),
                    parts[4].isBlank() ? null : Double.parseDouble(parts[4]),
                    parts[5].isBlank() ? null : parts[5]
            );
        } catch (NumberFormatException exception) {
            throw invalid();
        }
    }

    public String fingerprint(String targetToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(targetToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private String signature(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(SIGNING_KEY, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }

    public record TargetTokenPayload(
            Long targetMemberId,
            Integer rank,
            String nickname,
            Double walletBalance,
            Double profitRate,
            String leaderboardMode
    ) {
    }
}
