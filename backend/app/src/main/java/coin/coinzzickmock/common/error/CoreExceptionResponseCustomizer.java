package coin.coinzzickmock.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface CoreExceptionResponseCustomizer {
    void customize(CoreException exception, HttpServletRequest request, ResponseEntity.BodyBuilder response);
}
