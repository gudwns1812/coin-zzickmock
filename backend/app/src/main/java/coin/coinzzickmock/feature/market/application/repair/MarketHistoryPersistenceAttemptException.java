package coin.coinzzickmock.feature.market.application.repair;

public class MarketHistoryPersistenceAttemptException extends RuntimeException {
    public MarketHistoryPersistenceAttemptException(String message) {
        super(message);
    }

    public MarketHistoryPersistenceAttemptException(String message, Throwable cause) {
        super(message, cause);
    }
}
