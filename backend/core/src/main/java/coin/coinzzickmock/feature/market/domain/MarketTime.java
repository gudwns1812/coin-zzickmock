package coin.coinzzickmock.feature.market.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public final class MarketTime {
    public static final ZoneOffset STORAGE_ZONE = ZoneOffset.UTC;

    private MarketTime() {
    }

    public static ZonedDateTime atStorageZone(Instant instant) {
        return ZonedDateTime.ofInstant(instant, STORAGE_ZONE);
    }

    public static Instant truncate(Instant instant, ChronoUnit unit) {
        return atStorageZone(instant)
                .truncatedTo(unit)
                .toInstant();
    }

    public static Instant alignToMinuteBucket(Instant time, int bucketMinutes) {
        ZonedDateTime storageTime = atStorageZone(time).truncatedTo(ChronoUnit.MINUTES);
        int alignedMinute = (storageTime.getMinute() / bucketMinutes) * bucketMinutes;
        return storageTime.withMinute(alignedMinute)
                .toInstant();
    }

    public static Instant bucketStart(Instant time, MarketCandleInterval interval) {
        ZonedDateTime storageTime = atStorageZone(time);

        return switch (interval) {
            case FOUR_HOURS -> storageTime.truncatedTo(ChronoUnit.HOURS)
                    .withHour((storageTime.getHour() / 4) * 4)
                    .toInstant();
            case TWELVE_HOURS -> storageTime.truncatedTo(ChronoUnit.HOURS)
                    .withHour((storageTime.getHour() / 12) * 12)
                    .toInstant();
            case ONE_DAY -> storageTime.truncatedTo(ChronoUnit.DAYS).toInstant();
            case ONE_WEEK -> storageTime.truncatedTo(ChronoUnit.DAYS)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toInstant();
            case ONE_MONTH -> storageTime.truncatedTo(ChronoUnit.DAYS)
                    .withDayOfMonth(1)
                    .toInstant();
            default -> throw new CoreException(ErrorCode.INVALID_REQUEST);
        };
    }

    public static Instant bucketClose(Instant bucketStart, MarketCandleInterval interval) {
        ZonedDateTime storageTime = atStorageZone(bucketStart);

        return switch (interval) {
            case FOUR_HOURS -> storageTime.plusHours(4).toInstant();
            case TWELVE_HOURS -> storageTime.plusHours(12).toInstant();
            case ONE_DAY -> storageTime.plusDays(1).toInstant();
            case ONE_WEEK -> storageTime.plusWeeks(1).toInstant();
            case ONE_MONTH -> storageTime.plusMonths(1).toInstant();
            default -> throw new CoreException(ErrorCode.INVALID_REQUEST);
        };
    }
}
