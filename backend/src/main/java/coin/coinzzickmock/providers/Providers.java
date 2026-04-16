package coin.coinzzickmock.providers;

import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;

public interface Providers {
    AuthProvider auth();

    ConnectorProvider connector();

    TelemetryProvider telemetry();

    FeatureFlagProvider featureFlags();
}
