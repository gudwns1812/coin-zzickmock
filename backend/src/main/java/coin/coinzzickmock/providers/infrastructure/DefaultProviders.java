package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import org.springframework.stereotype.Component;

@Component
public class DefaultProviders implements Providers {
    private final AuthProvider authProvider;
    private final ConnectorProvider connectorProvider;
    private final TelemetryProvider telemetryProvider;
    private final FeatureFlagProvider featureFlagProvider;

    public DefaultProviders(
            AuthProvider authProvider,
            ConnectorProvider connectorProvider,
            TelemetryProvider telemetryProvider,
            FeatureFlagProvider featureFlagProvider
    ) {
        this.authProvider = authProvider;
        this.connectorProvider = connectorProvider;
        this.telemetryProvider = telemetryProvider;
        this.featureFlagProvider = featureFlagProvider;
    }

    @Override
    public AuthProvider auth() {
        return authProvider;
    }

    @Override
    public ConnectorProvider connector() {
        return connectorProvider;
    }

    @Override
    public TelemetryProvider telemetry() {
        return telemetryProvider;
    }

    @Override
    public FeatureFlagProvider featureFlags() {
        return featureFlagProvider;
    }
}
