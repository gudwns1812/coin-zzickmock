package coin.coinzzickmock.providers.infrastructure;

import java.util.List;

public record BitgetCandleResponse(
        String code,
        String msg,
        List<List<String>> data
) {
}
