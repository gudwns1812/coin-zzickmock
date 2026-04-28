package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.infrastructure.mapper.BitgetTickerSnapshotMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BitgetMarketDataGatewayTest {
    @Test
    void mapsTickerUsdtVolumeToMarketTurnover() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.bitget.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BitgetMarketDataGateway gateway = new BitgetMarketDataGateway(
                builder.build(),
                new BitgetTickerSnapshotMapper()
        );
        server.expect(requestTo("https://api.bitget.com/api/v2/mix/market/ticker"
                        + "?symbol=BTCUSDT&productType=USDT-FUTURES"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "code": "00000",
                          "msg": "success",
                          "data": [
                            {
                              "symbol": "BTCUSDT",
                              "lastPr": "74000",
                              "markPrice": "74010",
                              "indexPrice": "74005",
                              "fundingRate": "0.0001",
                              "change24h": "0.2",
                              "usdtVolume": "5250000000"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        MarketSnapshot market = gateway.loadMarket("BTCUSDT");

        server.verify();
        assertThat(market.turnover24hUsdt()).isEqualTo(5_250_000_000d);
    }

    @Test
    void loadsHistoricalCandlesFromBitgetHistoryEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.bitget.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BitgetMarketDataGateway gateway = new BitgetMarketDataGateway(
                builder.build(),
                new BitgetTickerSnapshotMapper()
        );
        server.expect(requestTo("https://api.bitget.com/api/v2/mix/market/history-candles"
                        + "?symbol=BTCUSDT&productType=USDT-FUTURES&granularity=1H"
                        + "&startTime=1580515200000&endTime=1580522400000&limit=200"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("symbol", "BTCUSDT"))
                .andExpect(queryParam("productType", "USDT-FUTURES"))
                .andExpect(queryParam("granularity", "1H"))
                .andExpect(queryParam("startTime", "1580515200000"))
                .andExpect(queryParam("endTime", "1580522400000"))
                .andExpect(queryParam("limit", "200"))
                .andRespond(withSuccess("""
                        {
                          "code": "00000",
                          "msg": "success",
                          "data": [
                            ["1580515200000", "100", "110", "90", "105", "12", "1260"]
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<MarketHistoricalCandleSnapshot> candles = gateway.loadHistoricalCandles(
                "BTCUSDT",
                MarketCandleInterval.ONE_HOUR,
                Instant.parse("2020-02-01T00:00:00Z"),
                Instant.parse("2020-02-01T02:00:00Z"),
                500
        );

        server.verify();
        assertThat(candles).hasSize(1);
        assertThat(candles.get(0).openTime()).isEqualTo(Instant.parse("2020-02-01T00:00:00Z"));
        assertThat(candles.get(0).closeTime()).isEqualTo(Instant.parse("2020-02-01T01:00:00Z"));
        assertThat(candles.get(0).quoteVolume()).isEqualTo(1260);
    }
}
