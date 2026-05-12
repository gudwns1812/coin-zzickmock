package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = CoinZzickmockApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "spring.task.scheduling.enabled=false"
)
@AutoConfigureObservability
@ActiveProfiles("test")
class ObservabilityActuatorEndpointTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TelemetryProvider telemetryProvider;

    @Test
    void exposesPrometheusScrapeEndpointWithCommonTags() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(telemetryProvider).isInstanceOf(MicrometerTelemetryProvider.class);
        assertThat(response.getBody())
                .contains("# HELP")
                .contains("application=\"coin-zzickmock\"")
                .contains("service=\"backend\"")
                .contains("environment=\"local\"");
    }

    @Test
    void doesNotExposeMetricsDiscoveryEndpointInPrOne() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("/actuator/prometheus")
                .doesNotContain("/actuator/metrics");
    }
}
