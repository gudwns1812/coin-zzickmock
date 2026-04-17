package coin.coinzzickmock.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ApplicationServiceDependencyRuleTest {
    private static final Path SOURCE_ROOT = Path.of("src/main/java/coin/coinzzickmock/feature");

    @Test
    void applicationServicesMustNotDependOnOtherApplicationServices() throws IOException {
        List<ServiceSource> services = loadApplicationServices();
        List<String> violations = new ArrayList<>();

        for (ServiceSource source : services) {
            List<String> lines = Files.readAllLines(source.path(), StandardCharsets.UTF_8);
            for (ServiceSource candidate : services) {
                if (source.equals(candidate)) {
                    continue;
                }

                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(candidate.simpleName()) + "\\b");
                for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                    String line = lines.get(lineIndex);
                    String trimmed = line.trim();
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                        continue;
                    }
                    if (pattern.matcher(line).find()) {
                        violations.add(source.relativePath() + ":" + (lineIndex + 1)
                                + " -> " + candidate.simpleName());
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "application/service 유스케이스는 다른 service를 직접 참조하면 안 됩니다.\n"
                        + String.join("\n", violations)
        );
    }

    private List<ServiceSource> loadApplicationServices() throws IOException {
        try (var stream = Files.walk(SOURCE_ROOT)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().contains("/application/service/"))
                    .filter(path -> path.getFileName().toString().endsWith("Service.java"))
                    .map(ServiceSource::from)
                    .toList();
        }
    }

    private record ServiceSource(Path path, String simpleName, String relativePath) {
        private static ServiceSource from(Path path) {
            String simpleName = path.getFileName().toString().replace(".java", "");
            String relativePath = path.toString().replace('\\', '/');
            return new ServiceSource(path, simpleName, relativePath);
        }
    }
}
