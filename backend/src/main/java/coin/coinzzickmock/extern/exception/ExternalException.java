package coin.coinzzickmock.extern.exception;

import coin.coinzzickmock.support.error.ErrorType;

public class ExternalException extends RuntimeException {

    private final ErrorType errorType;

    public ExternalException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    public ExternalException(ErrorType errorType, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
