package coin.coinzzickmock.providers.featureflag;

public interface FeatureFlagProvider {
    boolean isEnabled(String key);
}
