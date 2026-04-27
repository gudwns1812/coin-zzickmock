package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.application.realtime.MarketFundingScheduleLookup;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import java.time.DateTimeException;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketFundingSchedulePersistenceLookup implements MarketFundingScheduleLookup {
    private final MarketSymbolEntityRepository marketSymbolEntityRepository;

    @Override
    public FundingSchedule scheduleFor(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return FundingSchedule.defaultUsdtPerpetual();
        }
        return marketSymbolEntityRepository.findBySymbol(symbol)
                .map(this::toSchedule)
                .orElseGet(FundingSchedule::defaultUsdtPerpetual);
    }

    private FundingSchedule toSchedule(MarketSymbolEntity symbol) {
        try {
            return new FundingSchedule(
                    symbol.fundingIntervalHours(),
                    symbol.fundingAnchorHour(),
                    toZoneId(symbol)
            );
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "Invalid funding schedule for symbol {}; interval={}, anchorHour={}; falling back to default",
                    symbol.symbol(),
                    symbol.fundingIntervalHours(),
                    symbol.fundingAnchorHour(),
                    exception
            );
            return FundingSchedule.defaultUsdtPerpetual();
        }
    }

    private ZoneId toZoneId(MarketSymbolEntity symbol) {
        try {
            return ZoneId.of(symbol.fundingTimeZone());
        } catch (DateTimeException | NullPointerException exception) {
            log.warn(
                    "Invalid funding time zone '{}' for symbol {}; falling back to {}",
                    symbol.fundingTimeZone(),
                    symbol.symbol(),
                    FundingSchedule.DEFAULT_ZONE_ID
            );
            return FundingSchedule.DEFAULT_ZONE_ID;
        }
    }
}
