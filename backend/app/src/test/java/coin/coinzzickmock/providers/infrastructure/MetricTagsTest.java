package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetricTagsTest {
    @Test
    void acceptsStableLowCardinalityTags() {
        Tags tags = MetricTags.of(Map.of(
                "symbol", "BTCUSDT",
                "interval", "1m",
                "range_bucket", "2026-04",
                "source", "login"
        ));

        assertThat(tags.stream().map(tag -> tag.getKey() + "=" + tag.getValue()))
                .containsExactly("interval=1m", "range_bucket=2026-04", "source=login", "symbol=BTCUSDT");
    }

    @Test
    void acceptsDauActivitySourceTags() {
        assertThat(MetricTags.of(Map.of(
                "source", "authenticated_api",
                "result", "success"
        ))).isNotEmpty();
    }

    @Test
    void acceptsAllSupportedCandleIntervalTags() {
        assertThat(Arrays.stream(MarketCandleInterval.values()).map(MarketCandleInterval::value))
                .isNotEmpty()
                .doesNotContainNull()
                .doesNotHaveDuplicates()
                .allSatisfy(interval -> assertThat(MetricTags.of(Map.of("interval", interval))
                        .stream()
                        .map(tag -> tag.getKey() + "=" + tag.getValue()))
                        .containsExactly("interval=" + interval));
    }

    @Test
    void rejectsForbiddenTagKeys() {
        assertThatThrownBy(() -> MetricTags.of(Map.of("memberId", "123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase snake_case");

        assertThatThrownBy(() -> MetricTags.of(Map.of("member_id", "123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("forbidden");

        assertThatThrownBy(() -> MetricTags.of(Map.of("user_id", "123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("forbidden");

        assertThatThrownBy(() -> MetricTags.of(Map.of("account_id", "123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("forbidden");

        assertThatThrownBy(() -> MetricTags.of(Map.of("session_id", "abc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("forbidden");
    }

    @Test
    void rejectsTagKeysOutsideMetricSchemaAllowList() {
        assertThatThrownBy(() -> MetricTags.of(Map.of("route", "api_futures_orders")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allow-listed");
    }

    @Test
    void rejectsUnstableRawValues() {
        assertThatThrownBy(() -> MetricTags.of(Map.of("source", "/api/futures/orders")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unstable");

        assertThatThrownBy(() -> MetricTags.of(Map.of(
                "source",
                "aaaaaaaaaaaaaaaaaaaa.bbbbbbbbbbbbbbbbbbbb.cccccccccccccccccccc"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unstable");

        assertThatThrownBy(() -> MetricTags.of(Map.of("reason", "java.lang.IllegalStateException: boom")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unstable");
    }

    @Test
    void acceptsStableRoutePatternsButRejectsRawQueries() {
        assertThat(MetricTags.of(Map.of("route_pattern", "/api/futures/markets/{symbol}/candles")))
                .isNotEmpty();

        assertThatThrownBy(() -> MetricTags.of(Map.of(
                "route_pattern",
                "/api/futures/markets/BTCUSDT/candles?before=123"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unstable");

        assertThatThrownBy(() -> MetricTags.of(Map.of(
                "route_pattern",
                "/api/futures/markets/BTCUSDT/candles"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unstable");

        assertThatThrownBy(() -> MetricTags.of(Map.of(
                "route_pattern",
                "/api/futures/orders/123/cancel"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unstable");
    }

    @Test
    void requiresLowercaseDotNotationMeterNames() {
        assertThat(MetricTags.meterName("market.history.db.hit")).isEqualTo("market.history.db.hit");

        assertThatThrownBy(() -> MetricTags.meterName("MarketHistoryHit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase dot notation");
    }
}
