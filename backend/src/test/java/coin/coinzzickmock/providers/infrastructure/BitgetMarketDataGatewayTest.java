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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
                        + "&startTime=1580515200000&endTime=1580522400000&limit=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("symbol", "BTCUSDT"))
                .andExpect(queryParam("productType", "USDT-FUTURES"))
                .andExpect(queryParam("granularity", "1H"))
                .andExpect(queryParam("startTime", "1580515200000"))
                .andExpect(queryParam("endTime", "1580522400000"))
                .andExpect(queryParam("limit", "2"))
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

    @Test
    void splitsHistoricalCandlesIntoNinetyDayBatches() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.bitget.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BitgetMarketDataGateway gateway = new BitgetMarketDataGateway(
                builder.build(),
                new BitgetTickerSnapshotMapper()
        );
        Instant fromInclusive = Instant.parse("2025-01-01T00:00:00Z");
        Instant firstBatchEndExclusive = Instant.parse("2025-04-01T00:00:00Z");
        Instant secondBatchEndExclusive = Instant.parse("2025-06-30T00:00:00Z");
        Instant toExclusive = Instant.parse("2025-08-29T00:00:00Z");

        expectHistoricalRequest(server, "1Dutc", fromInclusive, firstBatchEndExclusive, "90", """
                {
                  "code": "00000",
                  "msg": "success",
                  "data": [
                    ["1735689600000", "100", "110", "90", "105", "12", "1260"]
                  ]
                }
                """);
        expectHistoricalRequest(server, "1Dutc", firstBatchEndExclusive, secondBatchEndExclusive, "90", """
                {
                  "code": "00000",
                  "msg": "success",
                  "data": [
                    ["1743465600000", "110", "120", "100", "115", "11", "1265"]
                  ]
                }
                """);
        expectHistoricalRequest(server, "1Dutc", secondBatchEndExclusive, toExclusive, "60", """
                {
                  "code": "00000",
                  "msg": "success",
                  "data": [
                    ["1751241600000", "120", "130", "110", "125", "10", "1250"]
                  ]
                }
                """);

        List<MarketHistoricalCandleSnapshot> candles = gateway.loadHistoricalCandles(
                "BTCUSDT",
                MarketCandleInterval.ONE_DAY,
                fromInclusive,
                toExclusive,
                240
        );

        server.verify();
        assertThat(candles).extracting(MarketHistoricalCandleSnapshot::openTime)
                .containsExactly(
                        Instant.parse("2025-01-01T00:00:00Z"),
                        Instant.parse("2025-04-01T00:00:00Z"),
                        Instant.parse("2025-06-30T00:00:00Z")
                );
    }

    @Test
    void usesUtcGranularityForWeeklyAndMonthlyHistoricalCandles() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.bitget.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BitgetMarketDataGateway gateway = new BitgetMarketDataGateway(
                builder.build(),
                new BitgetTickerSnapshotMapper()
        );

        expectHistoricalRequest(
                server,
                "1Wutc",
                Instant.parse("2025-01-06T00:00:00Z"),
                Instant.parse("2025-03-31T00:00:00Z"),
                "12",
                successfulEmptyResponse()
        );
        expectHistoricalRequest(
                server,
                "1Wutc",
                Instant.parse("2025-03-31T00:00:00Z"),
                Instant.parse("2025-05-26T00:00:00Z"),
                "8",
                successfulEmptyResponse()
        );
        expectHistoricalRequest(
                server,
                "1Mutc",
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-04-01T00:00:00Z"),
                "3",
                successfulEmptyResponse()
        );
        expectHistoricalRequest(
                server,
                "1Mutc",
                Instant.parse("2025-04-01T00:00:00Z"),
                Instant.parse("2025-06-01T00:00:00Z"),
                "2",
                successfulEmptyResponse()
        );
        expectHistoricalRequest(
                server,
                "1Mutc",
                Instant.parse("2025-06-01T00:00:00Z"),
                Instant.parse("2025-07-01T00:00:00Z"),
                "1",
                successfulEmptyResponse()
        );

        gateway.loadHistoricalCandles(
                "BTCUSDT",
                MarketCandleInterval.ONE_WEEK,
                Instant.parse("2025-01-06T00:00:00Z"),
                Instant.parse("2025-05-26T00:00:00Z"),
                20
        );
        gateway.loadHistoricalCandles(
                "BTCUSDT",
                MarketCandleInterval.ONE_MONTH,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-07-01T00:00:00Z"),
                6
        );

        server.verify();
    }

    @Test
    void clampsFutureHistoricalEndTimeToCurrentClosedBoundary() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.bitget.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BitgetMarketDataGateway gateway = new BitgetMarketDataGateway(
                builder.build(),
                new BitgetTickerSnapshotMapper(),
                Clock.fixed(Instant.parse("2026-04-29T19:12:53Z"), ZoneOffset.UTC)
        );

        expectHistoricalRequest(
                server,
                "1Dutc",
                Instant.parse("2026-04-22T00:00:00Z"),
                Instant.parse("2026-04-29T00:00:00Z"),
                "7",
                successfulEmptyResponse()
        );

        gateway.loadHistoricalCandles(
                "BTCUSDT",
                MarketCandleInterval.ONE_DAY,
                Instant.parse("2026-04-22T00:00:00Z"),
                Instant.parse("2026-11-08T00:00:00Z"),
                200
        );

        server.verify();
    }

    private String epochMillis(Instant instant) {
        return String.valueOf(instant.toEpochMilli());
    }

    private void expectHistoricalRequest(
            MockRestServiceServer server,
            String granularity,
            Instant fromInclusive,
            Instant toExclusive,
            String limit,
            String responseBody
    ) {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/api/v2/mix/market/history-candles"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("symbol", "BTCUSDT"))
                .andExpect(queryParam("productType", "USDT-FUTURES"))
                .andExpect(queryParam("granularity", granularity))
                .andExpect(queryParam("startTime", epochMillis(fromInclusive)))
                .andExpect(queryParam("endTime", epochMillis(toExclusive)))
                .andExpect(queryParam("limit", limit))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private String successfulEmptyResponse() {
        return """
                {
                  "code": "00000",
                  "msg": "success",
                  "data": []
                }
                """;
    }
}
