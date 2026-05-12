package coin.coinzzickmock.common.error;

public class CoreException extends RuntimeException {
    private final ErrorCode errorCode;

    public CoreException(ErrorCode errorCode) {
        super(message(errorCode));
        this.errorCode = errorCode;
    }

    private static String message(ErrorCode errorCode) {
        if (errorCode == null) {
            throw new IllegalArgumentException("errorCode must not be null");
        }
        return errorCode.message();
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
