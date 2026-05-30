package coin.coinzzickmock.common.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private static final String COOKIE_NAME = "oauth2AuthorizationRequest";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String COOKIE_VALUE_DELIMITER = ".";
    private static final int MAX_CONSUMED_STATE_COUNT = 10_000;

    private final boolean secureCookie;
    private final String sameSite;
    private final Duration ttl;
    private final byte[] signingKey;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ConcurrentMap<String, Long> consumedStateExpirations = new ConcurrentHashMap<>();

    public CookieOAuth2AuthorizationRequestRepository(
            @Value("${APP_AUTH_COOKIE_SECURE:true}") boolean secureCookie,
            @Value("${APP_AUTH_COOKIE_SAME_SITE:None}") String sameSite,
            @Value("${app.auth.google.authorization-cookie-ttl:PT5M}") Duration ttl,
            @Value("${app.auth.jwt-secret}") String jwtSecret,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
        this.ttl = ttl;
        this.signingKey = signingKey(jwtSecret);
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = cookie(request);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return null;
        }
        try {
            String[] parts = cookie.getValue().split("\\" + COOKIE_VALUE_DELIMITER, 2);
            if (parts.length != 2 || !signatureMatches(parts[0], parts[1])) {
                return null;
            }
            byte[] bytes = Base64.getUrlDecoder().decode(parts[0]);
            AuthorizationRequestCookie authorizationRequestCookie =
                    objectMapper.readValue(bytes, AuthorizationRequestCookie.class);
            long nowEpochMillis = clock.millis();
            cleanupExpiredConsumedStates(nowEpochMillis);
            if (authorizationRequestCookie.isExpired(nowEpochMillis)
                    || isConsumedState(authorizationRequestCookie.state(), nowEpochMillis)) {
                return null;
            }
            return authorizationRequestCookie.toAuthorizationRequest();
        } catch (IOException | IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            expire(response);
            return;
        }
        byte[] bytes = serialize(authorizationRequest);
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        response.addHeader("Set-Cookie", cookie(payload + COOKIE_VALUE_DELIMITER + signature(payload), ttl).toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            consumeState(authorizationRequest.getState());
        }
        expire(response);
        return authorizationRequest;
    }

    private Cookie cookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    private void expire(HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookie("", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private String signature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("OAuth authorization request cookie signer is unavailable.", exception);
        }
    }

    private boolean signatureMatches(String payload, String providedSignature) {
        return MessageDigest.isEqual(
                signature(payload).getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private byte[] signingKey(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    private boolean isConsumedState(String state, long nowEpochMillis) {
        String stateHash = stateHash(state);
        Long expiresAtEpochMillis = consumedStateExpirations.get(stateHash);
        if (expiresAtEpochMillis == null) {
            return false;
        }
        if (expiresAtEpochMillis <= nowEpochMillis) {
            consumedStateExpirations.remove(stateHash, expiresAtEpochMillis);
            return false;
        }
        return true;
    }

    private void consumeState(String state) {
        long nowEpochMillis = clock.millis();
        cleanupExpiredConsumedStates(nowEpochMillis);
        trimConsumedStateOverflow();
        consumedStateExpirations.put(stateHash(state), nowEpochMillis + ttl.toMillis());
    }

    private void cleanupExpiredConsumedStates(long nowEpochMillis) {
        consumedStateExpirations.entrySet()
                .removeIf(entry -> entry.getValue() <= nowEpochMillis);
    }

    private void trimConsumedStateOverflow() {
        while (consumedStateExpirations.size() >= MAX_CONSUMED_STATE_COUNT) {
            String stateHash = consumedStateExpirations.keySet().iterator().next();
            consumedStateExpirations.remove(stateHash);
        }
    }

    private String stateHash(String state) {
        return signature(state == null ? "" : state);
    }

    private byte[] serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            long issuedAtEpochMillis = clock.millis();
            long expiresAtEpochMillis = issuedAtEpochMillis + ttl.toMillis();
            return objectMapper.writeValueAsBytes(AuthorizationRequestCookie.from(
                    authorizationRequest,
                    issuedAtEpochMillis,
                    expiresAtEpochMillis
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("OAuth authorization request cookie cannot be serialized.", exception);
        }
    }

    private record AuthorizationRequestCookie(
            String authorizationUri,
            String clientId,
            String redirectUri,
            Set<String> scopes,
            String state,
            Map<String, Object> additionalParameters,
            Map<String, Object> attributes,
            String authorizationRequestUri,
            long issuedAtEpochMillis,
            long expiresAtEpochMillis
    ) {
        private static AuthorizationRequestCookie from(
                OAuth2AuthorizationRequest authorizationRequest,
                long issuedAtEpochMillis,
                long expiresAtEpochMillis
        ) {
            return new AuthorizationRequestCookie(
                    authorizationRequest.getAuthorizationUri(),
                    authorizationRequest.getClientId(),
                    authorizationRequest.getRedirectUri(),
                    authorizationRequest.getScopes(),
                    authorizationRequest.getState(),
                    authorizationRequest.getAdditionalParameters(),
                    authorizationRequest.getAttributes(),
                    authorizationRequest.getAuthorizationRequestUri(),
                    issuedAtEpochMillis,
                    expiresAtEpochMillis
            );
        }

        private boolean isExpired(long nowEpochMillis) {
            return expiresAtEpochMillis <= nowEpochMillis;
        }

        private OAuth2AuthorizationRequest toAuthorizationRequest() {
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes)
                    .state(state)
                    .additionalParameters(additionalParameters)
                    .attributes(attributes)
                    .authorizationRequestUri(authorizationRequestUri)
                    .build();
        }
    }
}
