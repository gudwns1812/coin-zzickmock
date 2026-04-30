package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MicrometerSseTelemetryTest {
    @Test
    void recordsConnectionLifecycleAndActiveGauge() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerSseTelemetry telemetry = new MicrometerSseTelemetry(registry);

        telemetry.connectionOpened("market");
        telemetry.connectionOpened("market");
        telemetry.connectionClosed("market", "timeout");

        assertThat(registry.get("sse.connections.current")
                .tag("stream", "market")
                .gauge()
                .value()).isEqualTo(1);
        assertThat(registry.counter(
                "sse.connections.opened.total",
                "stream",
                "market"
        ).count()).isEqualTo(2);
        assertThat(registry.counter(
                "sse.connections.closed.total",
                "stream",
                "market",
                "reason",
                "timeout"
        ).count()).isEqualTo(1);
    }

    @Test
    void recordsRejectedConnectionAndSendResult() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerSseTelemetry telemetry = new MicrometerSseTelemetry(registry);

        telemetry.connectionRejected("trading_execution", "member_limit");
        telemetry.sendRecorded("trading_execution", "failure", Duration.ofMillis(3));
        telemetry.executorRejected("trading_execution");

        assertThat(registry.counter(
                "sse.connections.rejected.total",
                "stream",
                "trading_execution",
                "reason",
                "member_limit"
        ).count()).isEqualTo(1);
        assertThat(registry.counter(
                "sse.send.total",
                "stream",
                "trading_execution",
                "result",
                "failure"
        ).count()).isEqualTo(1);
        assertThat(registry.timer(
                "sse.send.duration",
                "stream",
                "trading_execution",
                "result",
                "failure"
        ).count()).isEqualTo(1);
        assertThat(registry.counter(
                "sse.executor.rejected.total",
                "stream",
                "trading_execution"
        ).count()).isEqualTo(1);
    }

    @Test
    void mapsUnknownValuesToBoundedBuckets() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerSseTelemetry telemetry = new MicrometerSseTelemetry(registry);

        telemetry.connectionRejected("market/BTCUSDT?memberId=1", "memberId=1");
        telemetry.sendRecorded("market/BTCUSDT?memberId=1", "ok-ish", Duration.ofMillis(1));

        assertThat(registry.counter(
                "sse.connections.rejected.total",
                "stream",
                "unknown",
                "reason",
                "unknown"
        ).count()).isEqualTo(1);
        assertThat(registry.counter(
                "sse.send.total",
                "stream",
                "unknown",
                "result",
                "failure"
        ).count()).isEqualTo(1);
    }
}
