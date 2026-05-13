package coin.coinzzickmock.feature.community.application.result;

import java.util.List;

public record CommunityImageValidationResult(List<String> objectKeys) {
    public CommunityImageValidationResult {
        objectKeys = objectKeys == null ? List.of() : List.copyOf(objectKeys);
    }
}
