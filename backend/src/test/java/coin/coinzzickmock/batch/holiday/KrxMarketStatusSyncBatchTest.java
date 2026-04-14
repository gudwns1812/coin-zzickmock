package coin.coinzzickmock.batch.holiday;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.anyInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import coin.coinzzickmock.core.application.market.MarketStatusService;
import coin.coinzzickmock.core.domain.market.MarketStatus;

@ExtendWith(MockitoExtension.class)
class KrxMarketStatusSyncBatchTest {

    @Mock
    private MarketStatusService marketStatusService;

    @Test
    void syncsMarketStatusAtScheduledExecution() {
        KrxMarketStatusSyncBatch batch = new KrxMarketStatusSyncBatch(marketStatusService, true, "Asia/Seoul");
        when(marketStatusService.syncTodayStatus()).thenReturn(MarketStatus.OPEN);

        batch.syncTodayStatus();

        verify(marketStatusService).syncTodayStatus();
    }

    @Test
    void doesNotSyncWhenInitIsDisabled() {
        KrxMarketStatusSyncBatch batch = new KrxMarketStatusSyncBatch(marketStatusService, false, "Asia/Seoul");

        batch.init();

        verify(marketStatusService, never()).syncHolidayCalendar(anyInt());
        verify(marketStatusService, never()).syncTodayStatus();
    }

    @Test
    void syncsImmediatelyWhenInitIsEnabled() {
        KrxMarketStatusSyncBatch batch = new KrxMarketStatusSyncBatch(marketStatusService, true, "Asia/Seoul");
        when(marketStatusService.syncTodayStatus()).thenReturn(MarketStatus.CLOSED);

        batch.init();

        verify(marketStatusService).syncHolidayCalendar(anyInt());
        verify(marketStatusService).syncTodayStatus();
    }

    @Test
    void keepsFlowEvenWhenSyncFails() {
        KrxMarketStatusSyncBatch batch = new KrxMarketStatusSyncBatch(marketStatusService, true, "Asia/Seoul");
        when(marketStatusService.syncTodayStatus()).thenThrow(new RuntimeException("krx unavailable"));

        batch.syncTodayStatus();

        verify(marketStatusService).syncTodayStatus();
    }

    @Test
    void keepsFlowEvenWhenHolidayCalendarSyncFails() {
        KrxMarketStatusSyncBatch batch = new KrxMarketStatusSyncBatch(marketStatusService, true, "Asia/Seoul");
        when(marketStatusService.syncTodayStatus()).thenReturn(MarketStatus.OPEN);
        doThrow(new RuntimeException("krx unavailable")).when(marketStatusService).syncHolidayCalendar(anyInt());

        batch.init();

        verify(marketStatusService).syncHolidayCalendar(anyInt());
        verify(marketStatusService).syncTodayStatus();
    }
}
