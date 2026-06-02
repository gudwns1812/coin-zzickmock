package coin.coinzzickmock.feature.community.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class GenerateCommunityImageUploadPresignedUrlCommandTest {
    @Test
    void trimsWebValidatedFields() {
        GenerateCommunityImageUploadPresignedUrlCommand command =
                new GenerateCommunityImageUploadPresignedUrlCommand(1L, " chart.png ", " image/png ", 1024L);

        assertThat(command.fileName()).isEqualTo("chart.png");
        assertThat(command.contentType()).isEqualTo("image/png");
    }
}
