package coin.coinzzickmock.feature.community.web.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record CommunityPostUpsertRequest(
        @NotBlank String category,
        @NotBlank String title,
        @NotNull JsonNode contentJson,
        Set<String> imageObjectKeys
) {
    public Set<String> safeImageObjectKeys() {
        return imageObjectKeys == null ? Set.of() : Set.copyOf(imageObjectKeys);
    }
}
