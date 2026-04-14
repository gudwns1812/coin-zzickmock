package coin.coinzzickmock.core.application.stock.result;

import coin.coinzzickmock.core.domain.stock.Stock;

public record StockInfoResult(
        String stockCode,
        String stockName,
        String categoryName,
        String price,
        String openPrice,
        String highPrice,
        String lowPrice,
        String marketName,
        String changeAmount,
        String sign,
        String changeRate,
        String volume,
        String volumeValue,
        String stockImage
) {

    public static StockInfoResult from(Stock stock) {
        return new StockInfoResult(
                stock.getStockCode(),
                stock.getName(),
                stock.getCategory(),
                stock.getPrice(),
                stock.getOpenPrice(),
                stock.getHighPrice(),
                stock.getLowPrice(),
                stock.getMarketName(),
                stock.getChangeAmount(),
                stock.getSign(),
                stock.getChangeRate(),
                stock.getVolume(),
                stock.getVolumeValue(),
                stock.getStockImage()
        );
    }
}
