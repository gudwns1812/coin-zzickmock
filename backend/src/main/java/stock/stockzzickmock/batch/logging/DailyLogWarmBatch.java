package stock.stockzzickmock.batch.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DailyLogWarmBatch {

    @Scheduled(cron = "1 0 0 * * *")
    public void warm() {
        log.info("하루가 시작되었습니다! 로그 초기화");
    }
}
