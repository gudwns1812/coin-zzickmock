package coin.coinzzickmock.providers.infrastructure;

import java.util.List;

public record BitgetTickerResponse(
        String code,
        String msg,
        List<BitgetTickerData> data
) {
    public BitgetTickerResponse {
        data = data == null ? List.of() : List.copyOf(data);
    }
}
