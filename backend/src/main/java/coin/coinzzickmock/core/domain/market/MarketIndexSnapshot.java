package coin.coinzzickmock.core.domain.market;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MarketIndexSnapshot {

    private final String date;
    private final String currentPrice;
    private final String highPrice;
    private final String lowPrice;
    private final String accumulatedVolume;
    private final String accumulatedVolumePrice;
}
