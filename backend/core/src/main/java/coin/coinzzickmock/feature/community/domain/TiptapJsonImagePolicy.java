package coin.coinzzickmock.feature.community.domain;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record TiptapJsonImagePolicy(Set<String> approvedObjectKeys, List<String> allowedSrcPrefixes) {
    public TiptapJsonImagePolicy {
        approvedObjectKeys = Set.copyOf(Objects.requireNonNull(approvedObjectKeys, "approvedObjectKeys"));
        if (approvedObjectKeys.isEmpty() || approvedObjectKeys.stream().anyMatch(key -> key == null || key.isBlank())) {
            throw new IllegalArgumentException("approvedObjectKeys must not be blank");
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
        if (!approvedObjectKeys.contains(objectKey)) {
            return false;
        }
        return allowedSrcPrefixes.stream().anyMatch(src::startsWith);
    }
}
