package coin.coinzzickmock.feature.community.web.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

public record CommunityPostUpsertRequest(String category, String title, JsonNode contentJson, Set<String> imageObjectKeys) {
    public Set<String> safeImageObjectKeys() {
        return imageObjectKeys == null ? Set.of() : Set.copyOf(imageObjectKeys);
    }
}
