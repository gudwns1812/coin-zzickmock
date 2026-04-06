package stock.stockzzickmock.support.response;

import stock.stockzzickmock.support.error.ErrorType;

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
