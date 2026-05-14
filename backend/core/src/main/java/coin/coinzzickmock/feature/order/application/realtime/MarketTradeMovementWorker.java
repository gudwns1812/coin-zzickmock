package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketTradeMovementWorker implements SmartLifecycle {
    private final MarketTradeMovementQueue queue;
    private final PendingOrderFillProcessor pendingOrderFillProcessor;
    private volatile boolean running;
    private Thread workerThread;

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        workerThread = new Thread(this::run, "market-trade-movement-worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @Override
    public void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public int processAvailable() {
        int processed = 0;
        while (queue.poll().map(this::process).orElse(false)) {
            processed++;
        }
        return processed;
    }

    private void run() {
        while (running) {
            try {
                process(queue.take());
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                if (running) {
                    log.warn("Market trade movement worker interrupted while running", interruptedException);
                }
                return;
            } catch (RuntimeException exception) {
                log.warn("Market trade movement processing failed", exception);
            }
        }
    }

    private boolean process(MarketTradePriceMovedEvent event) {
        pendingOrderFillProcessor.fillExecutablePendingOrders(event);
        return true;
    }
}
