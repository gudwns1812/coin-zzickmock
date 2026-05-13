package coin.coinzzickmock.feature.community.application;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record CommunityImageUploadSettings(
        Set<String> allowedContentTypes,
        long maxBytes,
        Duration presignTtl,
        String objectKeyPrefix
) {
    public static final long DEFAULT_MAX_BYTES = 5L * 1024L * 1024L;
    public static final Duration DEFAULT_PRESIGN_TTL = Duration.ofMinutes(10);
    public static final Set<String> DEFAULT_ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif"
    );
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    public CommunityImageUploadSettings {
        allowedContentTypes = allowedContentTypes == null || allowedContentTypes.isEmpty()
                ? DEFAULT_ALLOWED_CONTENT_TYPES
                : Set.copyOf(allowedContentTypes.stream()
                .map(CommunityImageUploadSettings::normalizeContentType)
                .toList());
        if (maxBytes <= 0 || presignTtl == null || presignTtl.isZero() || presignTtl.isNegative()
                || objectKeyPrefix == null || objectKeyPrefix.isBlank()) {
            throw invalid();
        }
        objectKeyPrefix = normalizePrefix(objectKeyPrefix);
        if (!EXTENSIONS.keySet().containsAll(allowedContentTypes)) {
            throw invalid();
        }
    }

    public static CommunityImageUploadSettings defaults() {
        return new CommunityImageUploadSettings(
                DEFAULT_ALLOWED_CONTENT_TYPES,
                DEFAULT_MAX_BYTES,
                DEFAULT_PRESIGN_TTL,
                "community"
        );
    }

    public void validate(String contentType, long sizeBytes) {
        String normalized = normalizeContentType(contentType);
        if (!allowedContentTypes.contains(normalized) || sizeBytes <= 0 || sizeBytes > maxBytes) {
            throw invalid();
        }
    }

    public String normalizeAllowedContentType(String contentType) {
        String normalized = normalizeContentType(contentType);
        if (!allowedContentTypes.contains(normalized)) {
            throw invalid();
        }
        return normalized;
    }

    public String extensionFor(String contentType) {
        String extension = EXTENSIONS.get(normalizeAllowedContentType(contentType));
        if (extension == null) {
            throw invalid();
        }
        return extension;
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw invalid();
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePrefix(String prefix) {
        String normalized = prefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank() || normalized.contains("..")) {
            throw invalid();
        }
        return normalized;
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
