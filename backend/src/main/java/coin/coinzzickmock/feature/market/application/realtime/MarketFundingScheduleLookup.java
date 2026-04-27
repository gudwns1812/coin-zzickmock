package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.domain.FundingSchedule;

public interface MarketFundingScheduleLookup {
    FundingSchedule scheduleFor(String symbol);
}
