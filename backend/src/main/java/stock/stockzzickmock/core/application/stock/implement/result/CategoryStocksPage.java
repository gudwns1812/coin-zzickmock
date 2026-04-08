package stock.stockzzickmock.core.application.stock.implement.result;

import java.util.List;
import stock.stockzzickmock.core.domain.stock.Stock;

public record CategoryStocksPage(
        int totalPages,
        List<Stock> stocks
) {
}
