package coin.coinzzickmock.providers.infrastructure;

import java.util.List;

public record BitgetTickerResponse(
        String code,
        String msg,
        List<BitgetTickerData> data
) {
}
