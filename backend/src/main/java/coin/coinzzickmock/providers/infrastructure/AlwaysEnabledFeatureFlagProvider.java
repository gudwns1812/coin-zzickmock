package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import org.springframework.stereotype.Component;

@Component
public class AlwaysEnabledFeatureFlagProvider implements FeatureFlagProvider {
    @Override
    public boolean isEnabled(String key) {
        return true;
    }
}
