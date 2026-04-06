package stock.stockzzickmock.batch.holiday;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.extern.holiday.HolidayApiClient;
import stock.stockzzickmock.storage.redis.publisher.HolidayToRedis;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class HolidaySyncBatch {

    private static final DateTimeFormatter REDIS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final HolidayApiClient holidayApiClient;
    private final HolidayToRedis publisher;

    @Value("${init.enabled:true}")
    private boolean initEnabled;

    private List<LocalDate> holidays = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (!initEnabled) {
            log.info("공휴일 초기화가 비활성화되어 있습니다.");
            return;
        }
        syncCurrentMonthHolidays();
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void syncCurrentMonthHolidays() {
        List<LocalDate> newDates = holidayApiClient.fetchCurrentMonthHolidays(LocalDate.now());
        if (newDates.isEmpty()) {
            log.error("공휴일을 추가하는데 에러 발생했습니다.");
            return;
        }

        List<LocalDate> addedDates = newDates.stream()
                .filter(date -> !holidays.contains(date))
                .toList();

        for (LocalDate addedDate : addedDates) {
            String dateStr = addedDate.format(REDIS_DATE_FORMAT);
            publisher.publish(dateStr);
            log.info("공휴일 추가 및 publish: {}", dateStr);
        }

        holidays = newDates;
    }
}
