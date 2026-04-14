package coin.coinzzickmock.core.application.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import coin.coinzzickmock.core.domain.market.MarketStatus;
import coin.coinzzickmock.extern.holiday.KrxHolidayClient;
import coin.coinzzickmock.storage.db.market.entity.KrxHolidayEntity;
import coin.coinzzickmock.storage.db.market.repository.KrxHolidayJpaRepository;
import coin.coinzzickmock.storage.redis.market.MarketStatusRedisRepository;
import coin.coinzzickmock.support.error.CoreException;
import coin.coinzzickmock.support.error.ExternalErrorType;

@ExtendWith(MockitoExtension.class)
class MarketStatusServiceTest {

    @Mock
    private KrxHolidayClient krxHolidayClient;

    @Mock
    private KrxHolidayJpaRepository krxHolidayJpaRepository;

    @Mock
    private MarketStatusRedisRepository marketStatusRedisRepository;

    private MarketStatusService marketStatusService;

    @BeforeEach
    void setUp() {
        marketStatusService = new MarketStatusService(
                krxHolidayClient,
                krxHolidayJpaRepository,
                marketStatusRedisRepository,
                "Asia/Seoul"
        );
    }

    @Test
    void storesClosedWhenTargetDateIsWeekend() {
        LocalDate saturday = LocalDate.of(2026, 4, 11);

        MarketStatus result = marketStatusService.syncStatus(saturday);

        assertThat(result).isEqualTo(MarketStatus.CLOSED);
        verify(krxHolidayClient, never()).fetchHolidays(2026);
        verify(marketStatusRedisRepository).setStatus(MarketStatus.CLOSED);
    }

    @Test
    void storesClosedWhenTargetDateIsHoliday() {
        LocalDate weekdayHoliday = LocalDate.of(2026, 5, 1);
        when(krxHolidayJpaRepository.existsByMarketCodeAndHolidayYear("KRX", 2026)).thenReturn(true);
        when(krxHolidayJpaRepository.existsByMarketCodeAndHolidayDate("KRX", weekdayHoliday)).thenReturn(true);

        MarketStatus result = marketStatusService.syncStatus(weekdayHoliday);

        assertThat(result).isEqualTo(MarketStatus.CLOSED);
        verify(krxHolidayClient, never()).fetchHolidays(2026);
        verify(marketStatusRedisRepository).setStatus(MarketStatus.CLOSED);
    }

    @Test
    void storesOpenWhenTargetDateIsBusinessDay() {
        LocalDate businessDay = LocalDate.of(2026, 4, 9);
        when(krxHolidayJpaRepository.existsByMarketCodeAndHolidayYear("KRX", 2026)).thenReturn(true);
        when(krxHolidayJpaRepository.existsByMarketCodeAndHolidayDate("KRX", businessDay)).thenReturn(false);

        MarketStatus result = marketStatusService.syncStatus(businessDay);

        assertThat(result).isEqualTo(MarketStatus.OPEN);
        verify(marketStatusRedisRepository).setStatus(MarketStatus.OPEN);
    }

    @Test
    void keepsPreviousRedisValuesWhenHolidayCalendarIsNotReady() {
        LocalDate businessDay = LocalDate.of(2026, 4, 9);
        when(krxHolidayJpaRepository.existsByMarketCodeAndHolidayYear("KRX", 2026)).thenReturn(false);

        assertThatThrownBy(() -> marketStatusService.syncStatus(businessDay))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ExternalErrorType.KRX_HOLIDAY_CALENDAR_NOT_READY);

        verify(marketStatusRedisRepository, never()).setStatus(MarketStatus.OPEN);
        verify(marketStatusRedisRepository, never()).setStatus(MarketStatus.CLOSED);
        verify(krxHolidayJpaRepository, never()).saveAll(any());
        verify(krxHolidayClient, never()).fetchHolidays(2026);
    }

    @Test
    void syncsHolidayCalendarByYear() {
        Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 1, 1));
        when(krxHolidayClient.fetchHolidays(2026)).thenReturn(holidays);

        marketStatusService.syncHolidayCalendar(2026);

        verify(krxHolidayJpaRepository).deleteByMarketCodeAndHolidayYear("KRX", 2026);
        verify(krxHolidayJpaRepository).saveAll(any());
    }

    @Test
    void savesHolidayDateWithKrxMarketCode() {
        Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 1, 1));
        when(krxHolidayClient.fetchHolidays(2026)).thenReturn(holidays);

        marketStatusService.syncHolidayCalendar(2026);

        ArgumentCaptor<Iterable<KrxHolidayEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(krxHolidayJpaRepository).saveAll(captor.capture());
        Iterator<KrxHolidayEntity> iterator = captor.getValue().iterator();
        assertThat(iterator.hasNext()).isTrue();
        KrxHolidayEntity entity = iterator.next();
        assertThat(entity.getMarketCode()).isEqualTo("KRX");
        assertThat(entity.getHolidayDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(iterator.hasNext()).isFalse();
    }
}
