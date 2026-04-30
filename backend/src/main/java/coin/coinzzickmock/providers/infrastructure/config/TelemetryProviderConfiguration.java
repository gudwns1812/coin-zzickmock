package coin.coinzzickmock.providers.infrastructure.config;

import coin.coinzzickmock.providers.infrastructure.MicrometerTelemetryProvider;
import coin.coinzzickmock.providers.infrastructure.NoopTelemetryProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelemetryProviderConfiguration {
    @Bean
    TelemetryProvider telemetryProvider(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            return new NoopTelemetryProvider();
        }
        return new MicrometerTelemetryProvider(meterRegistry);
    }
}
