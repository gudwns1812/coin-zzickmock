package coin.coinzzickmock.providers.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BitgetWebSocketMarketEventParser {
    private final ObjectMapper objectMapper;

    public BitgetWebSocketMarketEventParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public List<BitgetWebSocketMarketEvent> parse(String payload, Instant receivedAt) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                return List.of();
            }

            JsonNode arg = root.path("arg");
            String channelName = arg.path("channel").asText();
            String symbol = firstText(arg.path("instId"), data.get(0).path("instId"), data.get(0).path("symbol"));
            if (symbol == null || symbol.isBlank()) {
                return List.of();
            }

            return BitgetWebSocketChannel.from(channelName)
                    .map(channel -> parseChannel(channel, symbol, root, data, receivedAt))
                    .orElseGet(List::of);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<BitgetWebSocketMarketEvent> parseChannel(
            BitgetWebSocketChannel channel,
            String symbol,
            JsonNode root,
            JsonNode data,
            Instant receivedAt
    ) {
        return switch (channel) {
            case TRADE -> parseTrades(symbol, data, receivedAt);
            case TICKER -> parseTickers(symbol, root, data, receivedAt);
            case CANDLE_1M, CANDLE_1H -> parseCandles(symbol, channel, root, data, receivedAt);
        };
    }

    private List<BitgetWebSocketMarketEvent> parseTrades(String symbol, JsonNode data, Instant receivedAt) {
        List<BitgetWebSocketMarketEvent> events = new ArrayList<>();
        for (JsonNode item : data) {
            String tradeId = item.path("tradeId").asText(null);
            BigDecimal price = decimal(item.path("price"));
            BigDecimal size = decimal(item.path("size"));
            Instant sourceEventTime = instant(item.path("ts"));
            String side = item.path("side").asText(null);
            if (price == null || size == null || sourceEventTime == null) {
                continue;
            }
            events.add(new BitgetWebSocketTradeEvent(
                    symbol,
                    tradeId,
                    price,
                    size,
                    side,
                    sourceEventTime,
                    receivedAt
            ));
        }
        return events;
    }

    private List<BitgetWebSocketMarketEvent> parseTickers(
            String symbol,
            JsonNode root,
            JsonNode data,
            Instant receivedAt
    ) {
        List<BitgetWebSocketMarketEvent> events = new ArrayList<>();
        for (JsonNode item : data) {
            BigDecimal lastPrice = decimal(item.path("lastPr"));
            BigDecimal markPrice = decimal(item.path("markPrice"));
            BigDecimal indexPrice = decimal(item.path("indexPrice"));
            BigDecimal fundingRate = decimal(item.path("fundingRate"));
            Instant nextFundingTime = instant(item.path("nextFundingTime"));
            Instant sourceEventTime = instant(item.path("ts"));
            if (sourceEventTime == null) {
                sourceEventTime = instant(root.path("ts"));
            }
            if (lastPrice == null || markPrice == null || indexPrice == null || sourceEventTime == null) {
                continue;
            }
            events.add(new BitgetWebSocketTickerEvent(
                    symbol,
                    lastPrice,
                    markPrice,
                    indexPrice,
                    fundingRate,
                    nextFundingTime,
                    sourceEventTime,
                    receivedAt
            ));
        }
        return events;
    }

    private List<BitgetWebSocketMarketEvent> parseCandles(
            String symbol,
            BitgetWebSocketChannel channel,
            JsonNode root,
            JsonNode data,
            Instant receivedAt
    ) {
        List<BitgetWebSocketMarketEvent> events = new ArrayList<>();
        Instant sourceEventTime = instant(root.path("ts"));
        for (JsonNode item : data) {
            if (!item.isArray() || item.size() < 8 || sourceEventTime == null) {
                continue;
            }
            Instant openTime = instant(item.get(0));
            BigDecimal openPrice = decimal(item.get(1));
            BigDecimal highPrice = decimal(item.get(2));
            BigDecimal lowPrice = decimal(item.get(3));
            BigDecimal closePrice = decimal(item.get(4));
            BigDecimal baseVolume = decimal(item.get(5));
            BigDecimal quoteVolume = decimal(item.get(6));
            BigDecimal usdtVolume = decimal(item.get(7));
            if (openTime == null || openPrice == null || highPrice == null || lowPrice == null || closePrice == null
                    || baseVolume == null || quoteVolume == null || usdtVolume == null) {
                continue;
            }
            events.add(new BitgetWebSocketCandleEvent(
                    symbol,
                    channel.interval(),
                    openTime,
                    openPrice,
                    highPrice,
                    lowPrice,
                    closePrice,
                    baseVolume,
                    quoteVolume,
                    usdtVolume,
                    sourceEventTime,
                    receivedAt
            ));
        }
        return events;
    }

    private static String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull() && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        return null;
    }

    private static BigDecimal decimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Instant instant(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(node.asText()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
