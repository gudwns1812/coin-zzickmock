package coin.coinzzickmock.core.application.stock.result;

import java.util.List;

public record CategoryPageResult(
        int totalPages,
        List<StockSummaryResult> stocks
) {
}
