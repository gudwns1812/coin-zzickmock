package coin.coinzzickmock.feature.market.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.application.dto.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketCandlesService;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketControllerTest {
    @Test
    void mapsFundingScheduleFieldsToMarketSummaryResponse() {
        Instant serverTime = Instant.parse("2026-04-26T23:59:30Z");
        Instant nextFundingAt = Instant.parse("2026-04-27T00:00:00Z");
        MarketSummaryResult market = new MarketSummaryResult(
                "BTCUSDT",
                "Bitcoin Perpetual",
                74000,
                74010,
                74005,
                0.0001,
                0.2,
                6_400_000_000d,
                serverTime,
                nextFundingAt,
                8
        );

        MarketSummaryHttpResponse response = MarketHttpResponseMapper.toResponse(market);

        assertTrue(response.turnover24hUsdt() == 6_400_000_000d);
        assertTrue(response.volume24h() == 6_400_000_000d);
        assertTrue(response.serverTime().equals(serverTime));
        assertTrue(response.nextFundingAt().equals(nextFundingAt));
        assertTrue(response.fundingIntervalHours() == 8);
    }

    @Test
    void passesBeforeCursorToCandleQuery() {
        GetMarketSummaryService summaryService = mock(GetMarketSummaryService.class);
        GetMarketCandlesService candleService = mock(GetMarketCandlesService.class);
        MarketController controller = new MarketController(summaryService, candleService);
        Instant before = Instant.parse("2026-04-21T00:05:00Z");

        when(candleService.getCandles(org.mockito.Mockito.any())).thenReturn(List.of());

        controller.candles("BTCUSDT", "1m", 120, before);

        verify(candleService).getCandles(argThat(query ->
                query.symbol().equals("BTCUSDT")
                        && query.interval() == coin.coinzzickmock.feature.market.domain.MarketCandleInterval.ONE_MINUTE
                        && query.limit() == 120
                        && before.equals(query.beforeOpenTime())
        ));
    }
}
