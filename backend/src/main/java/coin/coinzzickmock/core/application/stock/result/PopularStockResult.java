package coin.coinzzickmock.core.application.stock.result;

import coin.coinzzickmock.core.domain.market.PopularStock;

public record PopularStockResult(
        String stockName,
        String stockCode,
        String rank,
        String price,
        String sign,
        String changeAmount,
        String changeRate,
        String stockImage
) {

    public static PopularStockResult from(PopularStock stock) {
        return new PopularStockResult(
                stock.getStockName(),
                stock.getStockCode(),
                stock.getRank(),
                stock.getPrice(),
                stock.getSign(),
                stock.getChangeAmount(),
                stock.getChangeRate(),
                stock.getStockImage()
        );
    }
}
