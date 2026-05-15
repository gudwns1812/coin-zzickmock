package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class RealtimeMarketDataStore {
    private static final int DEFAULT_ACCEPTED_TRADE_IDS_PER_SYMBOL = 10_000;

    private final AtomicLong receiveSequence = new AtomicLong();
    private final Map<String, RealtimeMarketTradeState> trades = new ConcurrentHashMap<>();
    private final Map<String, BoundedTradeIdSet> acceptedTradeIds = new ConcurrentHashMap<>();
    private final Map<String, RealtimeMarketTickerState> tickers = new ConcurrentHashMap<>();
    private final Map<CandleKey, RealtimeMarketCandleState> candles = new ConcurrentHashMap<>();
    private final Map<SourceKey, MarketRealtimeSourceSnapshot> fallbackSources = new ConcurrentHashMap<>();
    private final int acceptedTradeIdLimit;

    public RealtimeMarketDataStore() {
        this(DEFAULT_ACCEPTED_TRADE_IDS_PER_SYMBOL);
    }

    RealtimeMarketDataStore(int acceptedTradeIdLimit) {
        if (acceptedTradeIdLimit <= 0) {
            throw new IllegalArgumentException("acceptedTradeIdLimit must be positive");
        }
        this.acceptedTradeIdLimit = acceptedTradeIdLimit;
    }

    public boolean acceptTrade(RealtimeMarketTradeTick tick) {
        return acceptTradeUpdate(tick).accepted();
    }

    public AcceptedTradeUpdate acceptTradeUpdate(RealtimeMarketTradeTick tick) {
        long sequence = receiveSequence.incrementAndGet();
        BoundedTradeIdSet tradeIds = null;
        boolean reservedTradeId = false;
        if (tick.tradeId() != null && !tick.tradeId().isBlank()) {
            tradeIds = tradeIds(tick.symbol());
            reservedTradeId = tradeIds.reserve(tick.tradeId());
            if (!reservedTradeId) {
                return AcceptedTradeUpdate.rejected();
            }
        }

        RealtimeMarketTradeState next = new RealtimeMarketTradeState(
                tick.symbol(),
                tick.tradeId(),
                tick.price(),
                tick.size(),
                tick.side(),
                sequence,
                MarketRealtimeSourceSnapshot.webSocket(
                        tick.symbol(),
                        MarketRealtimeSourceType.TRADE,
                        tick.sourceEventTime(),
                        tick.receivedAt(),
                        tick.tradeId(),
                        null
                )
        );
        AtomicReference<MarketTradePriceMovedEvent> movement = new AtomicReference<>();
        AtomicBoolean accepted = new AtomicBoolean(false);
        trades.compute(tick.symbol(), (symbol, previous) -> {
            if (!shouldReplaceTimestamped(previous, next)) {
                return previous;
            }
            accepted.set(true);
            if (previous != null) {
                double previousPrice = previous.price().doubleValue();
                double currentPrice = next.price().doubleValue();
                MarketPriceMovementDirection direction = MarketPriceMovementDirection.between(previousPrice,
                        currentPrice);
                if (direction != MarketPriceMovementDirection.UNCHANGED) {
                    movement.set(new MarketTradePriceMovedEvent(
                            symbol,
                            previousPrice,
                            currentPrice,
                            direction,
                            tick.sourceEventTime(),
                            tick.receivedAt()
                    ));
                }
            }
            return next;
        });
        if (reservedTradeId) {
            if (accepted.get()) {
                tradeIds.commit();
            } else {
                tradeIds.remove(tick.tradeId());
            }
        }
        return new AcceptedTradeUpdate(accepted.get(), Optional.ofNullable(movement.get()));
    }

    public boolean acceptTicker(RealtimeMarketTickerUpdate update) {
        long sequence = receiveSequence.incrementAndGet();
        RealtimeMarketTickerState next = new RealtimeMarketTickerState(
                update.symbol(),
                update.lastPrice(),
                update.markPrice(),
                update.indexPrice(),
                update.fundingRate(),
                update.nextFundingTime(),
                sequence,
                MarketRealtimeSourceSnapshot.webSocket(
                        update.symbol(),
                        MarketRealtimeSourceType.TICKER,
                        update.sourceEventTime(),
                        update.receivedAt(),
                        null,
                        null
                )
        );
        tickers.compute(update.symbol(),
                (symbol, previous) -> shouldReplaceTimestamped(previous, next) ? next : previous);
        return tickers.get(update.symbol()) == next;
    }

    public boolean acceptCandle(RealtimeMarketCandleUpdate update) {
        long sequence = receiveSequence.incrementAndGet();
        CandleKey key = new CandleKey(update.symbol(), update.interval(), update.openTime());
        RealtimeMarketCandleState next = new RealtimeMarketCandleState(
                update.symbol(),
                update.interval(),
                update.openTime(),
                update.openPrice(),
                update.highPrice(),
                update.lowPrice(),
                update.closePrice(),
                update.baseVolume(),
                update.quoteVolume(),
                update.usdtVolume(),
                MarketRealtimeSourceSnapshot.webSocket(
                        update.symbol(),
                        MarketRealtimeSourceType.CANDLE,
                        update.sourceEventTime(),
                        update.receivedAt(),
                        null,
                        update.openTime()
                )
        );
        candles.compute(key, (ignored, previous) -> {
            if (previous == null) {
                return next;
            }
            if (previous.source().isRestFallbackSource()) {
                return next;
            }
            if (previous.source().sourceEventTime().isAfter(update.sourceEventTime())) {
                return previous;
            }
            return next;
        });
        return candles.get(key) == next && sequence > 0;
    }

    public boolean acceptBootstrapCandle(RealtimeMarketCandleUpdate update) {
        long sequence = receiveSequence.incrementAndGet();
        CandleKey key = new CandleKey(update.symbol(), update.interval(), update.openTime());
        RealtimeMarketCandleState next = new RealtimeMarketCandleState(
                update.symbol(),
                update.interval(),
                update.openTime(),
                update.openPrice(),
                update.highPrice(),
                update.lowPrice(),
                update.closePrice(),
                update.baseVolume(),
                update.quoteVolume(),
                update.usdtVolume(),
                MarketRealtimeSourceSnapshot.restFallback(
                        update.symbol(),
                        MarketRealtimeSourceType.REST_BOOTSTRAP,
                        MarketRealtimeHealth.BOOTSTRAPPING,
                        update.sourceEventTime(),
                        update.receivedAt(),
                        "current_candle_bootstrap"
                )
        );
        candles.compute(key, (ignored, previous) -> {
            if (previous == null) {
                return next;
            }
            if (previous.source().isWebSocketSource()) {
                return previous;
            }
            if (previous.source().sourceEventTime().isAfter(update.sourceEventTime())) {
                return previous;
            }
            return next;
        });
        return candles.get(key) == next && sequence > 0;
    }

    public void recordRestBootstrap(String symbol, Instant sourceEventTime, Instant receivedAt, String fallbackReason) {
        recordFallback(symbol, MarketRealtimeSourceType.REST_BOOTSTRAP, MarketRealtimeHealth.BOOTSTRAPPING,
                sourceEventTime, receivedAt, fallbackReason);
    }

    public void recordRestRecovery(String symbol, Instant sourceEventTime, Instant receivedAt, String fallbackReason) {
        recordFallback(symbol, MarketRealtimeSourceType.REST_RECOVERY, MarketRealtimeHealth.RECOVERING,
                sourceEventTime, receivedAt, fallbackReason);
    }

    public Optional<RealtimeMarketTradeState> latestTrade(String symbol) {
        return Optional.ofNullable(trades.get(symbol));
    }

    public Optional<RealtimeMarketTickerState> latestTicker(String symbol) {
        return Optional.ofNullable(tickers.get(symbol));
    }

    public Optional<RealtimeMarketCandleState> candle(String symbol, MarketCandleInterval interval, Instant openTime) {
        return Optional.ofNullable(candles.get(new CandleKey(symbol, interval, openTime)));
    }

    public Optional<RealtimeMarketCandleState> latestCandle(String symbol, MarketCandleInterval interval) {
        return candles.entrySet().stream()
                .filter(entry -> entry.getKey().symbol().equals(symbol))
                .filter(entry -> entry.getKey().interval() == interval)
                .map(Map.Entry::getValue)
                .max((first, second) -> first.openTime().compareTo(second.openTime()));
    }

    public java.util.List<RealtimeMarketCandleState> candles(
            String symbol,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return candles.entrySet().stream()
                .filter(entry -> entry.getKey().symbol().equals(symbol))
                .filter(entry -> entry.getKey().interval() == interval)
                .map(Map.Entry::getValue)
                .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                .filter(candle -> candle.openTime().isBefore(toExclusive))
                .sorted(java.util.Comparator.comparing(RealtimeMarketCandleState::openTime))
                .toList();
    }

    public Optional<MarketRealtimeSourceSnapshot> fallbackSource(String symbol, MarketRealtimeSourceType sourceType) {
        return Optional.ofNullable(fallbackSources.get(new SourceKey(symbol, sourceType)));
    }

    private BoundedTradeIdSet tradeIds(String symbol) {
        return acceptedTradeIds.computeIfAbsent(symbol,
                ignored -> new BoundedTradeIdSet(acceptedTradeIdLimit));
    }

    private boolean shouldReplaceTimestamped(TimestampedRealtimeState previous, TimestampedRealtimeState next) {
        if (previous == null) {
            return true;
        }
        int timeComparison = next.source().sourceEventTime().compareTo(previous.source().sourceEventTime());
        if (timeComparison > 0) {
            return true;
        }
        return timeComparison == 0 && next.receiveSequence() > previous.receiveSequence();
    }

    private void recordFallback(
            String symbol,
            MarketRealtimeSourceType sourceType,
            MarketRealtimeHealth health,
            Instant sourceEventTime,
            Instant receivedAt,
            String fallbackReason
    ) {
        fallbackSources.put(
                new SourceKey(symbol, sourceType),
                MarketRealtimeSourceSnapshot.restFallback(symbol, sourceType, health, sourceEventTime, receivedAt,
                        fallbackReason)
        );
    }

    private record CandleKey(String symbol, MarketCandleInterval interval, Instant openTime) {
    }

    private record SourceKey(String symbol, MarketRealtimeSourceType sourceType) {
    }

    private static final class BoundedTradeIdSet {
        private final int limit;
        private final Set<String> ids = ConcurrentHashMap.newKeySet();
        private final Queue<String> insertionOrder = new ConcurrentLinkedQueue<>();

        private BoundedTradeIdSet(int limit) {
            this.limit = limit;
        }

        private boolean reserve(String tradeId) {
            boolean added = ids.add(tradeId);
            if (added) {
                insertionOrder.add(tradeId);
            }
            return added;
        }

        private void commit() {
            trim();
        }

        private void remove(String tradeId) {
            ids.remove(tradeId);
            insertionOrder.remove(tradeId);
        }

        private void trim() {
            while (ids.size() > limit) {
                String oldest = insertionOrder.poll();
                if (oldest == null) {
                    return;
                }
                ids.remove(oldest);
            }
        }
    }

    private interface TimestampedRealtimeState {
        long receiveSequence();

        MarketRealtimeSourceSnapshot source();
    }

    public record RealtimeMarketTradeState(
            String symbol,
            String tradeId,
            BigDecimal price,
            BigDecimal size,
            String side,
            long receiveSequence,
            MarketRealtimeSourceSnapshot source
    ) implements TimestampedRealtimeState {
    }

    public record AcceptedTradeUpdate(boolean accepted, Optional<MarketTradePriceMovedEvent> movement) {
        private static AcceptedTradeUpdate rejected() {
            return new AcceptedTradeUpdate(false, Optional.empty());
        }
    }

    public record RealtimeMarketTickerState(
            String symbol,
            BigDecimal lastPrice,
            BigDecimal markPrice,
            BigDecimal indexPrice,
            BigDecimal fundingRate,
            Instant nextFundingTime,
            long receiveSequence,
            MarketRealtimeSourceSnapshot source
    ) implements TimestampedRealtimeState {
    }
}
