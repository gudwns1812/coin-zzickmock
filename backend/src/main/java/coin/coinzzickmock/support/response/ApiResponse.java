package coin.coinzzickmock.support.response;

import coin.coinzzickmock.support.error.ErrorType;

public record ApiResponse<T>(
        ResultType result,
        T data,
        ErrorMessage<?> error
) {

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null);
    }

    public static <S> ApiResponse<S> error(S data, ErrorType errorType) {
        return new ApiResponse<>(ResultType.FAIL, null, ErrorMessage.from(errorType, data));
    }
}
