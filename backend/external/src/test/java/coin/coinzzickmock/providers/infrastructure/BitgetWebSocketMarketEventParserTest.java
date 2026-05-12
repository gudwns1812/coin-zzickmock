package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BitgetWebSocketMarketEventParserTest {
    private final BitgetWebSocketMarketEventParser parser = new BitgetWebSocketMarketEventParser(new ObjectMapper());
    private final Instant receivedAt = Instant.parse("2026-04-30T03:00:00Z");

    @Test
    void parsesPublicTradePayloads() {
        List<BitgetWebSocketMarketEvent> events = parser.parse("""
                {
                  "action": "snapshot",
                  "arg": {"instType": "USDT-FUTURES", "channel": "trade", "instId": "BTCUSDT"},
                  "data": [
                    {"ts": "1695716760565", "price": "27000.5", "size": "0.001", "side": "buy", "tradeId": "1111111111"}
                  ],
                  "ts": 1695716761589
                }
                """, receivedAt);

        assertThat(events).hasSize(1);
        BitgetWebSocketTradeEvent event = (BitgetWebSocketTradeEvent) events.get(0);
        assertThat(event.symbol()).isEqualTo("BTCUSDT");
        assertThat(event.tradeId()).isEqualTo("1111111111");
        assertThat(event.price()).isEqualByComparingTo(new BigDecimal("27000.5"));
        assertThat(event.sourceEventTime()).isEqualTo(Instant.ofEpochMilli(1695716760565L));
        assertThat(event.receivedAt()).isEqualTo(receivedAt);
    }

    @Test
    void parsesTickerPayloads() {
        List<BitgetWebSocketMarketEvent> events = parser.parse("""
                {
                  "data": [{
                    "lastPr": "87673.6",
                    "symbol": "BTCUSDT",
                    "indexPrice": "87714.0732915359034044",
                    "nextFundingTime": "1766678400000",
                    "change24h": "0.00743",
                    "quoteVolume": "1521198076.61216",
                    "instId": "BTCUSDT",
                    "markPrice": "87673.7",
                    "fundingRate": "0.000055",
                    "ts": "1766674540816"
                  }],
                  "arg": {"instType": "USDT-FUTURES", "instId": "BTCUSDT", "channel": "ticker"},
                  "action": "snapshot",
                  "ts": 1766674540817
                }
                """, receivedAt);

        assertThat(events).hasSize(1);
        BitgetWebSocketTickerEvent event = (BitgetWebSocketTickerEvent) events.get(0);
        assertThat(event.lastPrice()).isEqualByComparingTo(new BigDecimal("87673.6"));
        assertThat(event.markPrice()).isEqualByComparingTo(new BigDecimal("87673.7"));
        assertThat(event.indexPrice()).isEqualByComparingTo(new BigDecimal("87714.0732915359034044"));
        assertThat(event.nextFundingTime()).isEqualTo(Instant.ofEpochMilli(1766678400000L));
        assertThat(event.sourceEventTime()).isEqualTo(Instant.ofEpochMilli(1766674540816L));
    }

    @Test
    void parsesOneMinuteCandlePayloads() {
        List<BitgetWebSocketMarketEvent> events = parser.parse("""
                {
                  "action": "snapshot",
                  "arg": {"instType": "USDT-FUTURES", "channel": "candle1m", "instId": "BTCUSDT"},
                  "data": [["1695685500000", "27000", "27000.5", "26999.5", "27000.2", "0.057", "1539.0155", "1539.0155"]],
                  "ts": 1695715462250
                }
                """, receivedAt);

        assertThat(events).hasSize(1);
        BitgetWebSocketCandleEvent event = (BitgetWebSocketCandleEvent) events.get(0);
        assertThat(event.interval()).isEqualTo(ProviderMarketCandleInterval.ONE_MINUTE);
        assertThat(event.openTime()).isEqualTo(Instant.ofEpochMilli(1695685500000L));
        assertThat(event.highPrice()).isEqualByComparingTo(new BigDecimal("27000.5"));
        assertThat(event.lowPrice()).isEqualByComparingTo(new BigDecimal("26999.5"));
        assertThat(event.usdtVolume()).isEqualByComparingTo(new BigDecimal("1539.0155"));
    }

    @Test
    void parsesOneHourCandlePayloads() {
        List<BitgetWebSocketMarketEvent> events = parser.parse("""
                {
                  "action": "snapshot",
                  "arg": {"instType": "USDT-FUTURES", "channel": "candle1H", "instId": "BTCUSDT"},
                  "data": [["1695682800000", "27000", "27100", "26900", "27050", "3.2", "86400", "86400"]],
                  "ts": 1695715462250
                }
                """, receivedAt);

        assertThat(events).hasSize(1);
        BitgetWebSocketCandleEvent event = (BitgetWebSocketCandleEvent) events.get(0);
        assertThat(event.interval()).isEqualTo(ProviderMarketCandleInterval.ONE_HOUR);
    }

    @Test
    void ignoresSubscriptionAcksAndMalformedPayloads() {
        assertThat(parser.parse("""
                {"event": "subscribe", "arg": {"instType": "USDT-FUTURES", "channel": "trade", "instId": "BTCUSDT"}}
                """, receivedAt)).isEmpty();
        assertThat(parser.parse("{not-json", receivedAt)).isEmpty();
    }
}
