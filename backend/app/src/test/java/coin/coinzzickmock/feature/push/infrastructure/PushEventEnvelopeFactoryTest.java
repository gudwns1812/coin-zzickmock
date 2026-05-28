package coin.coinzzickmock.feature.push.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.order.application.dto.TradingExecutionEvent;
import coin.coinzzickmock.feature.market.web.MarketCandleResponse;
import coin.coinzzickmock.feature.push.application.dto.PushEventEnvelope;
import coin.coinzzickmock.feature.push.application.dto.PushEventType;
import coin.coinzzickmock.feature.push.application.dto.PushStream;
import coin.coinzzickmock.feature.push.application.dto.PushTargetType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PushEventEnvelopeFactoryTest {
    private final PushEventEnvelopeFactory factory = new PushEventEnvelopeFactory(
            new ObjectMapper().findAndRegisterModules(),
            new PushPublicationProperties("coin:push:market:v1", "coin:push:trading:v1", 10_000, Duration.ofSeconds(15), Duration.ofSeconds(5))
    );

    @Test
    void creates_trading_execution_envelope_without_member_id_in_payload() {
        TradingExecutionEvent event = TradingExecutionEvent.orderFilled(
                42L,
                "order-1",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                1.0,
                100.0
        );

        PushEventEnvelope envelope = factory.tradingExecution(event);

        assertThat(envelope.stream()).isEqualTo(PushStream.TRADING);
        assertThat(envelope.eventType()).isEqualTo(PushEventType.TRADING_EXECUTION);
        assertThat(envelope.targetType()).isEqualTo(PushTargetType.MEMBER);
        assertThat(envelope.memberId()).isEqualTo(42L);
        assertThat(envelope.payloadJson()).contains("ORDER_FILLED");
        assertThat(envelope.payloadJson()).doesNotContain("memberId");
    }

    @Test
    void candle_update_dedupe_key_changes_inside_the_same_open_bucket() {
        Instant openTime = Instant.parse("2026-05-29T00:00:00Z");
        MarketCandleResponse first = candle(openTime, 100.0, 101.0, 99.0, 100.5, 1.0);
        MarketCandleResponse second = candle(openTime, 100.0, 102.0, 99.0, 101.5, 2.0);

        PushEventEnvelope firstEnvelope = factory.unifiedMarketCandle("BTCUSDT", "1m", first);
        PushEventEnvelope secondEnvelope = factory.unifiedMarketCandle("BTCUSDT", "1m", second);

        assertThat(firstEnvelope.targetType()).isEqualTo(PushTargetType.SYMBOL_INTERVAL);
        assertThat(firstEnvelope.dedupeKey()).isNotEqualTo(secondEnvelope.dedupeKey());
        assertThat(firstEnvelope.dedupeKey()).contains("2026-05-29T00:00:00Z");
    }

    private static MarketCandleResponse candle(
            Instant openTime,
            double open,
            double high,
            double low,
            double close,
            double volume
    ) {
        return new MarketCandleResponse(
                openTime,
                openTime.plusSeconds(60),
                open,
                high,
                low,
                close,
                volume
        );
    }
}
