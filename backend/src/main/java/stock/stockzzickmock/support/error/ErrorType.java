package stock.stockzzickmock.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public interface ErrorType {

    String getErrorCode();

    String getMessage();

    LogLevel getLogLevel();

    HttpStatus getHttpStatus();
}
