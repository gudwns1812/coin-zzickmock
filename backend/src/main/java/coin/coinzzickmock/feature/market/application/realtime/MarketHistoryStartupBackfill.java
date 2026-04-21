package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketHistoryStartupBackfill {
    private static final ZoneOffset HISTORY_ZONE = ZoneOffset.UTC;
    private static final int MAX_MINUTE_CANDLES_PER_REQUEST = 1000;

    private final MarketHistoryRepository marketHistoryRepository;
    private final MarketHistoryRecorder marketHistoryRecorder;

    public void backfillMissingMinuteHistory(Instant observedAt, MarketDataGateway marketDataGateway) {
        List<MarketHistoryRepository.StartupBackfillCursor> backfillCursors = marketHistoryRepository.findStartupBackfillCursors();
        if (backfillCursors.isEmpty()) {
            return;
        }

        Instant currentMinuteOpenTime = truncate(observedAt, ChronoUnit.MINUTES);
        backfillCursors.forEach(cursor -> backfillSymbol(
                cursor.symbolId(),
                cursor.symbol(),
                cursor.latestPersistedMinuteOpenTime(),
                currentMinuteOpenTime,
                marketDataGateway
        ));
    }

    private void backfillSymbol(
            long symbolId,
            String symbol,
            Instant latestPersistedMinuteOpenTime,
            Instant currentMinuteOpenTime,
            MarketDataGateway marketDataGateway
    ) {
        if (latestPersistedMinuteOpenTime == null) {
            return;
        }

        Instant fetchFromInclusive = latestPersistedMinuteOpenTime.plus(1, ChronoUnit.MINUTES);
        if (!fetchFromInclusive.isBefore(currentMinuteOpenTime)) {
            return;
        }

        List<MarketMinuteCandleSnapshot> missingCandles = loadMissingMinuteCandles(
                symbol,
                fetchFromInclusive,
                currentMinuteOpenTime,
                marketDataGateway
        );
        marketHistoryRecorder.recordHistoricalMinuteCandles(symbolId, missingCandles);
    }

    private List<MarketMinuteCandleSnapshot> loadMissingMinuteCandles(
            String symbol,
            Instant fromInclusive,
            Instant toExclusive,
            MarketDataGateway marketDataGateway
    ) {
        List<MarketMinuteCandleSnapshot> loaded = new ArrayList<>();
        Instant cursor = fromInclusive;

        while (cursor.isBefore(toExclusive)) {
            Instant batchEndExclusive = min(
                    cursor.plus(MAX_MINUTE_CANDLES_PER_REQUEST, ChronoUnit.MINUTES),
                    toExclusive
            );
            loaded.addAll(marketDataGateway.loadMinuteCandles(symbol, cursor, batchEndExclusive));
            cursor = batchEndExclusive;
        }

        return loaded;
    }

    private Instant truncate(Instant instant, ChronoUnit unit) {
        return ZonedDateTime.ofInstant(instant, HISTORY_ZONE)
                .truncatedTo(unit)
                .toInstant();
    }

    private Instant min(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }
}
