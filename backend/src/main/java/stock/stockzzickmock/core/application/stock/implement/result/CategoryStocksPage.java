package stock.stockzzickmock.core.application.stock.implement.result;

import stock.stockzzickmock.core.domain.stock.Stock;

import java.util.List;

public record CategoryStocksPage(
        int totalPages,
        List<Stock> stocks
) {
}
