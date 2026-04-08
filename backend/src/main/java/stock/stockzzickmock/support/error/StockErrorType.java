package stock.stockzzickmock.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum StockErrorType implements ErrorType {

    INDICES_NOT_FOUND("STOCK_404_INDICES_NOT_FOUND", "지수 관련 정보를 찾지 못했습니다.", LogLevel.WARN, HttpStatus.NOT_FOUND),
    POPULAR_NOT_FOUND("STOCK_404_POPULAR_NOT_FOUND", "인기종목을 찾지 못했습니다.", LogLevel.WARN, HttpStatus.NOT_FOUND),
    STOCK_NOT_FOUND("STOCK_404_STOCK_NOT_FOUND", "주식을 찾지 못했습니다.", LogLevel.WARN, HttpStatus.NOT_FOUND),
    STOCK_ACTIVE_SET_PUBLISH_FAILED("STOCK_500_ACTIVE_SET_PUBLISH_FAILED", "활성 종목 집합을 적재하지 못했습니다.", LogLevel.ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR),
    STOCK_REQUEST_PUBLISH_FAILED("STOCK_500_REQUEST_PUBLISH_FAILED", "종목 수집 요청을 적재하지 못했습니다.", LogLevel.ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR),
    EMPTY_SEARCH_KEYWORD("STOCK_200_EMPTY_SEARCH_KEYWORD", "검색어가 비어있습니다.", LogLevel.INFO, HttpStatus.OK);

    private final String errorCode;
    private final String message;
    private final LogLevel logLevel;
    private final HttpStatus httpStatus;

    StockErrorType(String errorCode, String message, LogLevel logLevel, HttpStatus httpStatus) {
        this.errorCode = errorCode;
        this.message = message;
        this.logLevel = logLevel;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public LogLevel getLogLevel() {
        return logLevel;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
