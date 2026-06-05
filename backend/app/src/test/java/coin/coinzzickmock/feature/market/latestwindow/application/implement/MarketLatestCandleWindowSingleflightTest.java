package coin.coinzzickmock.feature.market.latestwindow.application.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.feature.market.candle.application.dto.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowPage;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MarketLatestCandleWindowSingleflightTest {
    @Test
    void executesSupplierOnceForConcurrentSameKeyCallers() throws Exception {
        MarketLatestCandleWindowSingleflight singleflight = new MarketLatestCandleWindowSingleflight();
        MarketLatestCandleWindowKey key = key("BTCUSDT", 120, "2026-04-21T00:00:00Z");
        MarketLatestCandleWindowPage page = page(key);
        AtomicInteger supplierCalls = new AtomicInteger();
        CountDownLatch supplierEntered = new CountDownLatch(1);
        CountDownLatch releaseSupplier = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MarketLatestCandleWindowPage> first = executor.submit(() -> singleflight.load(key, () -> {
                supplierCalls.incrementAndGet();
                supplierEntered.countDown();
                await(releaseSupplier);
                return page;
            }));
            supplierEntered.await();
            Future<MarketLatestCandleWindowPage> second = executor.submit(() -> singleflight.load(key, () -> {
                supplierCalls.incrementAndGet();
                return page(key);
            }));
            Thread.sleep(100);
            releaseSupplier.countDown();

            assertSame(page, first.get());
            assertSame(page, second.get());
            assertEquals(1, supplierCalls.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void clearsInflightAfterException() {
        MarketLatestCandleWindowSingleflight singleflight = new MarketLatestCandleWindowSingleflight();
        MarketLatestCandleWindowKey key = key("BTCUSDT", 120, "2026-04-21T00:00:00Z");
        AtomicInteger supplierCalls = new AtomicInteger();

        assertThrows(IllegalStateException.class, () -> singleflight.load(key, () -> {
            supplierCalls.incrementAndGet();
            throw new IllegalStateException("boom");
        }));
        MarketLatestCandleWindowPage recovered = singleflight.load(key, () -> {
            supplierCalls.incrementAndGet();
            return page(key);
        });

        assertEquals(key.latestOutputOpenTime(), recovered.latestOutputOpenTime());
        assertEquals(2, supplierCalls.get());
    }

    @Test
    void differentKeysExecuteIndependently() {
        MarketLatestCandleWindowSingleflight singleflight = new MarketLatestCandleWindowSingleflight();
        AtomicInteger supplierCalls = new AtomicInteger();

        singleflight.load(key("BTCUSDT", 120, "2026-04-21T00:00:00Z"), () -> {
            supplierCalls.incrementAndGet();
            return page(key("BTCUSDT", 120, "2026-04-21T00:00:00Z"));
        });
        singleflight.load(key("BTCUSDT", 180, "2026-04-21T00:00:00Z"), () -> {
            supplierCalls.incrementAndGet();
            return page(key("BTCUSDT", 180, "2026-04-21T00:00:00Z"));
        });

        assertEquals(2, supplierCalls.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static MarketLatestCandleWindowKey key(String symbol, int limit, String boundary) {
        return new MarketLatestCandleWindowKey(symbol, MarketCandleInterval.ONE_MINUTE, limit, Instant.parse(boundary));
    }

    private static MarketLatestCandleWindowPage page(MarketLatestCandleWindowKey key) {
        return new MarketLatestCandleWindowPage(
                List.of(new MarketCandleResult(
                        key.latestOutputOpenTime(),
                        key.latestOutputOpenTime().plusSeconds(60),
                        100,
                        101,
                        99,
                        100.5,
                        10
                )),
                key.interval(),
                key.limit(),
                key.latestOutputOpenTime(),
                Instant.parse("2026-04-21T00:01:00Z")
        );
    }
}
