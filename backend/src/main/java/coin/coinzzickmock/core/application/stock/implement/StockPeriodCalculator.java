package coin.coinzzickmock.core.application.stock.implement;

import org.springframework.stereotype.Component;
import coin.coinzzickmock.core.application.stock.result.StockPeriodResult;
import coin.coinzzickmock.core.domain.stock.StockHistory;

@Component
public class StockPeriodCalculator {

    public StockPeriodResult calculate(StockHistory stockHistory) {
        int prevPrice = Integer.parseInt(stockHistory.getClose()) - stockHistory.getPrevPrice();

        return new StockPeriodResult(
                stockHistory.getStockCode(),
                stockHistory.getDate(),
                stockHistory.getType(),
                stockHistory.getOpen(),
                stockHistory.getHigh(),
                stockHistory.getLow(),
                stockHistory.getClose(),
                stockHistory.getVolume(),
                stockHistory.getVolumeAmount(),
                prevPrice,
                Integer.parseInt(stockHistory.getOpen()) - prevPrice,
                stockHistory.getPrevPrice(),
                Integer.parseInt(stockHistory.getHigh()) - prevPrice,
                Integer.parseInt(stockHistory.getLow()) - prevPrice
        );
    }
}
