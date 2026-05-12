package coin.coinzzickmock.common.web;

public class SseSubscriptionLimitExceededException extends RuntimeException {
    private final String reason;

    public SseSubscriptionLimitExceededException(String reason) {
        super("SSE subscription limit exceeded: " + reason);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
