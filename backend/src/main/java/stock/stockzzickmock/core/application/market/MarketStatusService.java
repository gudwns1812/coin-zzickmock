package stock.stockzzickmock.core.application.market;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stock.stockzzickmock.core.domain.market.MarketStatus;
import stock.stockzzickmock.extern.holiday.KrxHolidayClient;
import stock.stockzzickmock.storage.db.market.entity.KrxHolidayEntity;
import stock.stockzzickmock.storage.db.market.repository.KrxHolidayJpaRepository;
import stock.stockzzickmock.storage.redis.market.MarketStatusRedisRepository;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.error.ExternalErrorType;

@Service
public class MarketStatusService {

    private static final String MARKET_CODE_KRX = "KRX";

    private final KrxHolidayClient krxHolidayClient;
    private final KrxHolidayJpaRepository krxHolidayJpaRepository;
    private final MarketStatusRedisRepository marketStatusRedisRepository;
    private final ZoneId zoneId;

    public MarketStatusService(
            KrxHolidayClient krxHolidayClient,
            KrxHolidayJpaRepository krxHolidayJpaRepository,
            MarketStatusRedisRepository marketStatusRedisRepository,
            @Value("${batch.market-status.krx.zone:Asia/Seoul}") String zone
    ) {
        this.krxHolidayClient = krxHolidayClient;
        this.krxHolidayJpaRepository = krxHolidayJpaRepository;
        this.marketStatusRedisRepository = marketStatusRedisRepository;
        this.zoneId = ZoneId.of(zone);
    }

    public MarketStatus syncTodayStatus() {
        return syncStatus(LocalDate.now(zoneId));
    }

    public MarketStatus syncStatus(LocalDate targetDate) {
        MarketStatus status = calculateStatus(targetDate);
        marketStatusRedisRepository.setStatus(status);
        return status;
    }

    @Transactional
    public void syncHolidayCalendar(int year) {
        List<KrxHolidayEntity> holidayEntities = krxHolidayClient.fetchHolidays(year).stream()
                .map(date -> KrxHolidayEntity.of(MARKET_CODE_KRX, date))
                .toList();

        krxHolidayJpaRepository.deleteByMarketCodeAndHolidayYear(MARKET_CODE_KRX, year);
        krxHolidayJpaRepository.saveAll(holidayEntities);
    }

    private MarketStatus calculateStatus(LocalDate targetDate) {
        if (isWeekend(targetDate)) {
            return MarketStatus.CLOSED;
        }

        ensureYearHolidaysPrepared(targetDate.getYear());
        return krxHolidayJpaRepository.existsByMarketCodeAndHolidayDate(MARKET_CODE_KRX, targetDate)
                ? MarketStatus.CLOSED
                : MarketStatus.OPEN;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private void ensureYearHolidaysPrepared(int year) {
        if (!krxHolidayJpaRepository.existsByMarketCodeAndHolidayYear(MARKET_CODE_KRX, year)) {
            throw new CoreException(ExternalErrorType.KRX_HOLIDAY_CALENDAR_NOT_READY);
        }
    }
}
