package coin.coinzzickmock.feature.community.domain;

import java.util.List;
import java.util.Objects;

public record TiptapJsonImagePolicy(String objectKeyPrefix, List<String> allowedSrcPrefixes) {
    public TiptapJsonImagePolicy {
        if (objectKeyPrefix == null || objectKeyPrefix.isBlank()) {
            throw new IllegalArgumentException("objectKeyPrefix must not be blank");
        }
        allowedSrcPrefixes = List.copyOf(Objects.requireNonNull(allowedSrcPrefixes, "allowedSrcPrefixes"));
        if (allowedSrcPrefixes.isEmpty() || allowedSrcPrefixes.stream().anyMatch(prefix -> prefix == null || prefix.isBlank())) {
            throw new IllegalArgumentException("allowedSrcPrefixes must not be blank");
        }
    }

    public boolean accepts(String objectKey, String src) {
        if (objectKey == null || src == null) {
            return false;
        }
        if (!objectKey.startsWith(objectKeyPrefix)) {
            return false;
        }
        return allowedSrcPrefixes.stream().anyMatch(src::startsWith);
    }
}
