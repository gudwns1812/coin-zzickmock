package coin.coinzzickmock.feature.community.application.dto;

import java.util.List;

public record CommunityImageValidationResult(List<String> objectKeys) {
    public CommunityImageValidationResult {
        objectKeys = objectKeys == null ? List.of() : List.copyOf(objectKeys);
    }
}
