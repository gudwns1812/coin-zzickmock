package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.application.realtime.MarketFundingScheduleLookup;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketFundingSchedulePersistenceLookup implements MarketFundingScheduleLookup {
    private final MarketSymbolEntityRepository marketSymbolEntityRepository;

    @Override
    public FundingSchedule scheduleFor(String symbol) {
        return marketSymbolEntityRepository.findBySymbol(symbol)
                .map(this::toSchedule)
                .orElseGet(FundingSchedule::defaultUsdtPerpetual);
    }

    private FundingSchedule toSchedule(MarketSymbolEntity symbol) {
        return new FundingSchedule(
                symbol.fundingIntervalHours(),
                symbol.fundingAnchorHourKst(),
                ZoneId.of(symbol.fundingTimeZone())
        );
    }
}
