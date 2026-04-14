package coin.coinzzickmock.core.application.stock.implement.result;

import java.util.List;
import coin.coinzzickmock.core.domain.stock.Stock;

public record CategoryStocksPage(
        int totalPages,
        List<Stock> stocks
) {
}
