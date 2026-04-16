package coin.coinzzickmock.providers.telemetry;

public interface TelemetryProvider {
    void recordUseCase(String useCaseName);

    void recordFailure(String useCaseName, String reason);
}
