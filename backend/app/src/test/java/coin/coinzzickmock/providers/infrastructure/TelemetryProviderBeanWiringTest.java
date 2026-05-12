package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.providers.infrastructure.config.TelemetryProviderConfiguration;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TelemetryProviderBeanWiringTest {
    @Test
    void usesNoopTelemetryWhenMeterRegistryIsMissing() {
        new ApplicationContextRunner()
                .withUserConfiguration(TelemetryProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(TelemetryProvider.class);
                    assertThat(context.getBean(TelemetryProvider.class)).isInstanceOf(NoopTelemetryProvider.class);
                });
    }

    @Test
    void usesMicrometerTelemetryWhenMeterRegistryExists() {
        new ApplicationContextRunner()
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withUserConfiguration(TelemetryProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(TelemetryProvider.class);
                    assertThat(context.getBean(TelemetryProvider.class))
                            .isInstanceOf(MicrometerTelemetryProvider.class);
                });
    }
}
