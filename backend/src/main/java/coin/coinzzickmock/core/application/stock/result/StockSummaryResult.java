package coin.coinzzickmock.core.application.stock.result;

import coin.coinzzickmock.core.domain.stock.Stock;

public record StockSummaryResult(
        String stockName,
        String stockCode,
        String currentPrice,
        String sign,
        String changeAmount,
        String changeRate,
        String stockImage
) {

    public static StockSummaryResult from(Stock stock) {
        return new StockSummaryResult(
                stock.getName(),
                stock.getStockCode(),
                stock.getPrice(),
                stock.getSign(),
                stock.getChangeAmount(),
                stock.getChangeRate(),
                stock.getStockImage()
        );
    }
}
