package stock.stockzzickmock.batch.holiday;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.application.market.MarketStatusService;
import stock.stockzzickmock.core.domain.market.MarketStatus;

@Component
@Slf4j
public class KrxMarketStatusSyncBatch {

    private final MarketStatusService marketStatusService;
    private final boolean initEnabled;
    private final ZoneId zoneId;

    public KrxMarketStatusSyncBatch(
            MarketStatusService marketStatusService,
            @Value("${batch.market-status.init.enabled:true}") boolean initEnabled,
            @Value("${batch.market-status.krx.zone:Asia/Seoul}") String zone
    ) {
        this.marketStatusService = marketStatusService;
        this.initEnabled = initEnabled;
        this.zoneId = ZoneId.of(zone);
    }

    @PostConstruct
    public void init() {
        if (!initEnabled) {
            log.info("KRX 장 상태 초기화가 비활성화되어 있습니다.");
            return;
        }
        syncCurrentYearHolidayCalendar();
        syncTodayStatus();
    }

    @Scheduled(
            cron = "${batch.market-status.krx.holiday-sync-cron:0 10 0 1 1 *}",
            zone = "${batch.market-status.krx.zone:Asia/Seoul}"
    )
    public void syncCurrentYearHolidayCalendar() {
        int currentYear = LocalDate.now(zoneId).getYear();
        try {
            marketStatusService.syncHolidayCalendar(currentYear);
            log.info("KRX 휴장일 캘린더를 동기화했습니다. year={}", currentYear);
        } catch (RuntimeException exception) {
            log.error("KRX 휴장일 캘린더 동기화에 실패했습니다. year={}", currentYear, exception);
        }
    }

    @Scheduled(
            cron = "${batch.market-status.krx.cron:0 0 8 * * *}",
            zone = "${batch.market-status.krx.zone:Asia/Seoul}"
    )
    public void syncTodayStatus() {
        try {
            MarketStatus status = marketStatusService.syncTodayStatus();
            log.info("KRX 장 상태를 갱신했습니다. status={}", status);
        } catch (RuntimeException exception) {
            log.error("KRX 장 상태 갱신에 실패하여 직전 Redis 값을 유지합니다.", exception);
        }
    }
}
