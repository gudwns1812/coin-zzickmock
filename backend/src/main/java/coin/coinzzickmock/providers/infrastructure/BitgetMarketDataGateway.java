package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.infrastructure.mapper.BitgetTickerSnapshotMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class BitgetMarketDataGateway implements MarketDataGateway {
    private final RestClient bitgetRestClient;
    private final BitgetTickerSnapshotMapper bitgetTickerSnapshotMapper;

    @Override
    public List<MarketSnapshot> loadSupportedMarkets() {
        return List.of(loadMarket("BTCUSDT"), loadMarket("ETHUSDT"));
    }

    @Override
    public MarketSnapshot loadMarket(String symbol) {
        try {
            BitgetTickerResponse response = bitgetRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/mix/market/ticker")
                            .queryParam("symbol", symbol)
                            .queryParam("productType", "USDT-FUTURES")
                            .build())
                    .retrieve()
                    .body(BitgetTickerResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                log.warn("Bitget ticker response is empty; using fallback market snapshot. symbol={} code={} message={}",
                        symbol, responseCode(response), responseMessage(response));
            }
            return bitgetTickerSnapshotMapper.fromResponse(symbol, response);
        } catch (Exception exception) {
            log.warn("Failed to load Bitget ticker; using fallback market snapshot. symbol={}", symbol, exception);
            return bitgetTickerSnapshotMapper.fallback(symbol);
        }
    }

    @Override
    public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
            String symbol,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        try {
            BitgetCandleResponse response = bitgetRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/mix/market/candles")
                            .queryParam("symbol", symbol)
                            .queryParam("productType", "USDT-FUTURES")
                            .queryParam("granularity", "1m")
                            .queryParam("startTime", fromInclusive.minus(1, ChronoUnit.MINUTES).toEpochMilli())
                            .queryParam("endTime", toExclusive.toEpochMilli())
                            .queryParam("limit", 1000)
                            .build())
                    .retrieve()
                    .body(BitgetCandleResponse.class);

            if (response == null || response.data() == null || !"00000".equals(response.code())) {
                log.warn(
                        "Bitget candle response is not usable; returning empty history. symbol={} from={} to={} code={} message={}",
                        symbol, fromInclusive, toExclusive, responseCode(response), responseMessage(response)
                );
                return List.of();
            }

            return response.data().stream()
                    .map(this::toMinuteCandleSnapshot)
                    .filter(Objects::nonNull)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(Comparator.comparing(MarketMinuteCandleSnapshot::openTime))
                    .toList();
        } catch (Exception exception) {
            log.warn("Failed to load Bitget minute candles; returning empty history. symbol={} from={} to={}",
                    symbol, fromInclusive, toExclusive, exception);
            return List.of();
        }
    }

    private MarketMinuteCandleSnapshot toMinuteCandleSnapshot(List<String> rawCandle) {
        if (rawCandle == null || rawCandle.size() < 7) {
            return null;
        }

        Instant openTime = Instant.ofEpochMilli(Long.parseLong(rawCandle.get(0)));
        return new MarketMinuteCandleSnapshot(
                openTime,
                openTime.plus(1, ChronoUnit.MINUTES),
                parseDouble(rawCandle.get(1)),
                parseDouble(rawCandle.get(2)),
                parseDouble(rawCandle.get(3)),
                parseDouble(rawCandle.get(4)),
                parseDouble(rawCandle.get(5)),
                parseDouble(rawCandle.get(6))
        );
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value);
    }

    private String responseCode(BitgetTickerResponse response) {
        return response == null ? null : response.code();
    }

    private String responseMessage(BitgetTickerResponse response) {
        return response == null ? null : response.msg();
    }

    private String responseCode(BitgetCandleResponse response) {
        return response == null ? null : response.code();
    }

    private String responseMessage(BitgetCandleResponse response) {
        return response == null ? null : response.msg();
    }
}
