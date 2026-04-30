package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = HttpRequestTelemetryFilterTest.TestApplication.class)
@AutoConfigureMockMvc
class HttpRequestTelemetryFilterTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void clearMeters() {
        meterRegistry.clear();
    }

    @Test
    void recordsBestMatchingRoutePatternWithoutRawPath() throws Exception {
        mockMvc.perform(get("/api/futures/test/123?memberId=456"))
                .andExpect(status().isOk());

        assertThat(meterRegistry.counter(
                "http.request.total",
                "method",
                "GET",
                "route_pattern",
                "/api/futures/test/{id}",
                "endpoint_group",
                "unknown",
                "status",
                "200",
                "status_family",
                "2xx"
        ).count()).isEqualTo(1);
        assertThat(meterRegistry.getMeters().stream()
                .flatMap(meter -> meter.getId().getTags().stream())
                .map(tag -> tag.getValue()))
                .doesNotContain("/api/futures/test/123", "/api/futures/test/123?memberId=456");
    }

    @Test
    void skipsSseStreamRoutes() throws Exception {
        mockMvc.perform(get("/api/futures/test/123/stream"))
                .andExpect(status().isOk());

        assertThat(meterRegistry.find("http.request.total").meters()).isEmpty();
    }

    @Test
    void preservesResponseBodyAfterRecordingTelemetry() throws Exception {
        mockMvc.perform(get("/api/futures/test/123"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        assertThat(meterRegistry.counter(
                "http.payload.size.bucket.total",
                "method",
                "GET",
                "route_pattern",
                "/api/futures/test/{id}",
                "endpoint_group",
                "unknown",
                "status_family",
                "2xx",
                "direction",
                "response",
                "size_bucket",
                "le_1kb"
        ).count()).isEqualTo(1);
    }

    @SpringBootConfiguration
    @Import({HttpRequestTelemetry.class, HttpRequestTelemetryFilter.class, TestController.class})
    static class TestApplication {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/api/futures/test/{id}")
        String read(@PathVariable String id) {
            return "ok";
        }

        @GetMapping(value = "/api/futures/test/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        String stream(@PathVariable String id) {
            return "event: ok\n\n";
        }
    }
}
