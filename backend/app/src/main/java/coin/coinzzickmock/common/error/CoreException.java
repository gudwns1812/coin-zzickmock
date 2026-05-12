package coin.coinzzickmock.common.error;

public class CoreException extends RuntimeException {
    private final ErrorCode errorCode;

    public CoreException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
