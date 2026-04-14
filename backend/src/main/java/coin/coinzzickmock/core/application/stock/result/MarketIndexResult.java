package coin.coinzzickmock.core.application.stock.result;

import coin.coinzzickmock.core.domain.market.MarketIndexSnapshot;

public record MarketIndexResult(
        String indiceDate,
        String curPrice,
        String highPrice,
        String lowPrice,
        String acmlVol,
        String acmlVolPrice
) {

    public static MarketIndexResult from(MarketIndexSnapshot snapshot) {
        return new MarketIndexResult(
                snapshot.getDate(),
                snapshot.getCurrentPrice(),
                snapshot.getHighPrice(),
                snapshot.getLowPrice(),
                snapshot.getAccumulatedVolume(),
                snapshot.getAccumulatedVolumePrice()
        );
    }
}
