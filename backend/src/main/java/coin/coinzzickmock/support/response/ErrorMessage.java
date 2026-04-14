package coin.coinzzickmock.support.response;

import coin.coinzzickmock.support.error.ErrorType;

public record ErrorMessage<T>(
        String errorCode,
        String message,
        T data
) {

    public static <T> ErrorMessage<T> from(ErrorType errorType, T data) {
        return new ErrorMessage<>(
                errorType.getErrorCode(),
                errorType.getMessage(),
                data
        );
    }
}
