package coin.coinzzickmock.feature.activity.domain;

public enum ActivitySource {
    LOGIN("login"),
    AUTHENTICATED_API("authenticated_api");

    private final String metricValue;

    ActivitySource(String metricValue) {
        this.metricValue = metricValue;
    }

    public String metricValue() {
        return metricValue;
    }
}
