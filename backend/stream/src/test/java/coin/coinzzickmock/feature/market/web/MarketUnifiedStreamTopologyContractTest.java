package coin.coinzzickmock.feature.market.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MarketUnifiedStreamTopologyContractTest {
    private static final Path MAIN = Path.of("src/main/java");
    private static final Path APP_MAIN = Path.of("../app/src/main/java");
    private static final Path CORE_MAIN = Path.of("../core/src/main/java");
    private static final Path MARKET_WEB = MAIN.resolve("coin/coinzzickmock/feature/market/web");
    private static final Path APP_MARKET_WEB = APP_MAIN.resolve("coin/coinzzickmock/feature/market/web");

    @Test
    void unifiedRegistryOwnsSessionSourceAndDerivedIndexesOnlyThroughApi() throws IOException {
        String registry = readRequired(MARKET_WEB.resolve("MarketStreamRegistry.java"));
        String session = readRequired(MARKET_WEB.resolve("MarketStreamSession.java"));
        readRequired(MARKET_WEB.resolve("MarketStreamSessionKey.java"));
        readRequired(MARKET_WEB.resolve("CandleSubscription.java"));
        readRequired(MARKET_WEB.resolve("SummarySubscriptionReason.java"));

        assertTrue(registry.contains("registerSession"), "registry must expose registerSession");
        assertTrue(registry.contains("releaseSession"), "registry must expose releaseSession");
        assertTrue(registry.contains("addSummaryReason"), "registry must expose addSummaryReason");
        assertTrue(registry.contains("removeSummaryReason"), "registry must expose removeSummaryReason");
        assertTrue(registry.contains("replaceCandleSubscription"), "registry must expose replaceCandleSubscription");
        assertTrue(registry.contains("sessionsForSummary"), "registry must expose sessionsForSummary fan-out snapshots");
        assertTrue(registry.contains("sessionsForCandle"), "registry must expose sessionsForCandle fan-out snapshots");
        assertTrue(registry.contains("private") && registry.contains("summaryIndex"),
                "summary fan-out index must be a private derived index");
        assertTrue(registry.contains("private") && registry.contains("candleIndex"),
                "candle fan-out index must be a private derived index");
        assertFalse(registry.contains(".send("), "registry must not send SSE events");
        assertFalse(registry.contains("onCompletion"), "registry must not own emitter lifecycle callbacks");
        assertFalse(registry.contains("onTimeout"), "registry must not own emitter lifecycle callbacks");
        assertFalse(registry.contains("onError"), "registry must not own emitter lifecycle callbacks");
        assertTrue(session.contains("SummarySubscriptionReason.ACTIVE_SYMBOL")
                        || session.contains("ACTIVE_SYMBOL"),
                "session must track active-symbol summary reason");
        assertTrue(session.contains("SummarySubscriptionReason.OPEN_POSITION")
                        || session.contains("OPEN_POSITION"),
                "session must track open-position summary reason");
        assertTrue(session.contains("CandleSubscription"),
                "candle state must be a single CandleSubscription, not a summary reason");
    }

    @Test
    void brokerOwnsUnifiedEnvelopeFanoutLifecycleFailureAndTelemetry() throws IOException {
        String broker = readRequired(MARKET_WEB.resolve("MarketStreamBroker.java"));
        readRequired(MARKET_WEB.resolve("MarketStreamEventResponse.java"));
        readRequired(MARKET_WEB.resolve("MarketStreamEventType.java"));

        assertTrue(broker.contains("onMarketUpdated(MarketSummaryResponse"), "broker must fan out market summary updates");
        assertTrue(broker.contains("onCandleUpdated(String symbol"), "broker must fan out candle updates");
        assertTrue(broker.contains("onHistoryFinalized(String symbol"), "broker must fan out history-finalized updates");
        assertTrue(broker.contains("sessionsForSummary"), "summary fan-out must come from registry snapshot");
        assertTrue(broker.contains("sessionsForCandle"), "candle fan-out must come from registry snapshot");
        assertTrue(broker.contains("SseEmitterLifecycle"),
                "broker must delegate common emitter lifecycle callback mechanics");
        assertTrue(broker.contains("releaseSession"), "broker must release registry sessions on lifecycle/send failure");
        assertTrue(broker.contains("SseTelemetry"), "broker must preserve SSE telemetry boundary");
        assertTrue(broker.contains("private static final String STREAM = \"market\""),
                "unified market stream must emit the documented market telemetry stream label");
        assertFalse(broker.contains("market_stream"),
                "undocumented stream labels are normalized to unknown in Micrometer telemetry");
        String envelope = readRequired(MARKET_WEB.resolve("MarketStreamEventResponse.java"));
        assertTrue(envelope.contains("MARKET_SUMMARY"), "unified envelopes must include market summary type");
        assertTrue(envelope.contains("MARKET_CANDLE"), "unified envelopes must include market candle type");
        assertTrue(envelope.contains("MARKET_HISTORY_FINALIZED"), "unified envelopes must include history finalized type");
        assertTrue(broker.contains("INITIAL_SNAPSHOT") || broker.contains("LIVE"),
                "envelopes must carry source metadata for initial/live de-duplication");
    }

    @Test
    void controllerAddsAuthenticatedUnifiedEndpointWithoutCouplingToPositionInternals() throws IOException {
        String controller = readRequired(APP_MARKET_WEB.resolve("MarketController.java"));
        String router = readRequired(MARKET_WEB.resolve("MarketSseStreamRouter.java"));
        String strategyContract = readRequired(MARKET_WEB.resolve("MarketSseStreamStrategy.java"));
        String strategy = readRequired(MARKET_WEB.resolve("UnifiedMarketStreamStrategy.java"));

        assertTrue(controller.contains("/stream"), "controller must expose unified /api/futures/markets/stream");
        assertTrue(controller.contains("MarketSseStreamRequest.summary"),
                "controller must build typed raw summary stream requests");
        assertTrue(controller.contains("MarketSseStreamRequest.candle"),
                "controller must build typed raw candle stream requests");
        assertTrue(controller.contains("MarketSseStreamRequest.unified"),
                "controller must build typed unified stream requests");
        assertTrue(controller.contains("MarketSseStreamRouter"),
                "controller must delegate SSE route selection to the market stream router");
        assertFalse(controller.contains("UnifiedMarketStreamOpener"),
                "controller must not depend on the removed unified stream opener");
        assertFalse(Files.exists(MARKET_WEB.resolve("UnifiedMarketStreamOpener.java")),
                "old unified stream opener source file must be removed");
        assertFalse(controller.contains("private final MarketRealtimeSseBroker marketRealtimeSseBroker"),
                "controller must not directly own the raw summary broker field");
        assertFalse(controller.contains("private final MarketCandleRealtimeSseBroker marketCandleRealtimeSseBroker"),
                "controller must not directly own the raw candle broker field");
        assertFalse(controller.contains("MarketStreamBroker marketStreamBroker"),
                "controller must not directly own the unified market stream broker field");
        assertTrue(router.contains("EnumMap"), "router must use explicit enum-key strategy wiring");
        assertTrue(router.contains("EnumSet.allOf"),
                "router must fail fast when any market SSE strategy kind is missing");
        assertFalse(router.contains("supports("), "router must not use service-locator supports matching");
        assertTrue(strategyContract.contains("MarketSseStreamKind kind();"),
                "strategy contract must expose an explicit enum kind");
        assertTrue(strategyContract.contains("void open(MarketSseStreamRequest request);"),
                "strategy contract must expose the stream open operation");
        assertFalse(strategyContract.contains("default "), "strategy contract must not define default behavior");
        assertTrue(strategy.contains("MarketStreamBroker"), "strategy must delegate unified stream lifecycle to broker");
        assertTrue(strategy.contains("MarketOpenPositionSymbolsReader"),
                "strategy must ask a narrow application-facing reader for open position symbols");
        assertFalse(controller.contains("PositionRepository"), "market web must not depend on position repositories");
        assertFalse(controller.contains("PositionJpa"), "market web must not depend on position persistence internals");
        assertTrue(strategy.contains("currentMemberId"),
                "unified endpoint must allow anonymous market viewers and enrich authenticated sessions only when present");
        assertFalse(strategy.contains("currentActor().memberId()"),
                "unified endpoint must not require authentication for public market/candle data");
        assertTrue(strategy.contains("Set.of()"),
                "anonymous unified sessions must omit only open-position summary symbols");
        assertTrue(controller.contains("clientKey"), "unified endpoint must be scoped by clientKey");
        assertTrue(controller.contains("MarketCandleInterval.from(interval)") || controller.contains("MarketSseStreamRequest.unified"),
                "unified endpoint must register active candle interval");
    }

    @Test
    void sseDeliveryExecutorHidesQualifiedExecutorFromFeatureBrokers() throws IOException {
        String deliveryConfiguration = readRequired(MAIN.resolve(
                "coin/coinzzickmock/common/web/SseDeliveryConfiguration.java"));
        String deliveryExecutor = readRequired(MAIN.resolve(
                "coin/coinzzickmock/common/web/SseDeliveryExecutor.java"));
        List<String> brokerSources = List.of(
                readRequired(MARKET_WEB.resolve("MarketStreamBroker.java")),
                readRequired(MARKET_WEB.resolve("MarketRealtimeSseBroker.java")),
                readRequired(MARKET_WEB.resolve("MarketCandleRealtimeSseBroker.java")),
                readRequired(MAIN.resolve("coin/coinzzickmock/feature/order/web/TradingExecutionSseBroker.java"))
        );

        assertTrue(deliveryConfiguration.contains("sseDeliveryTaskExecutor"),
                "backing SSE executor bean name must be delivery-neutral");
        assertTrue(deliveryExecutor.contains("@Qualifier(\"sseDeliveryTaskExecutor\")"),
                "qualifier must be isolated to common-web delivery wiring");
        for (String broker : brokerSources) {
            assertTrue(broker.contains("SseDeliveryExecutor"),
                    "feature SSE brokers must depend on the typed delivery executor");
            assertFalse(broker.contains("@Qualifier(\"marketRealtimeSseEventExecutor\")"),
                    "feature SSE brokers must not inject raw named executors");
        }
    }

    @Test
    void positionMutationsPublishBusinessSemanticEventsOnly() throws IOException {
        Path opened = findRequired("PositionOpenedEvent.java");
        Path fullyClosed = findRequired("PositionFullyClosedEvent.java");
        String openedSource = Files.readString(opened);
        String closedSource = Files.readString(fullyClosed);
        String openApplier = Files.readString(findRequired("FilledOpenOrderApplier.java"));
        String closeFinalizer = Files.readString(findRequired("PositionCloseFinalizer.java"));

        assertTrue(openedSource.contains("memberId") && openedSource.contains("symbol"),
                "PositionOpenedEvent must carry memberId and symbol");
        assertTrue(closedSource.contains("memberId") && closedSource.contains("symbol"),
                "PositionFullyClosedEvent must carry memberId and symbol");
        assertFalse(opened.toString().contains("Sse") || opened.toString().contains("Stream")
                        || opened.toString().contains("Subscription") || opened.toString().contains("Registry"),
                "position open event name must not expose delivery technology");
        assertFalse(fullyClosed.toString().contains("Sse") || fullyClosed.toString().contains("Stream")
                        || fullyClosed.toString().contains("Subscription") || fullyClosed.toString().contains("Registry"),
                "position close event name must not expose delivery technology");
        assertTrue(openApplier.contains("PositionOpenedEvent"),
                "new open-position creation must publish PositionOpenedEvent after commit");
        assertTrue(closeFinalizer.contains("PositionFullyClosedEvent"),
                "full close delete must publish PositionFullyClosedEvent after commit");
        assertTrue(openApplier.contains("AfterCommitEventPublisher"),
                "open position event must be published through after-commit infrastructure");
        assertTrue(closeFinalizer.contains("AfterCommitEventPublisher"),
                "close position event must be published through after-commit infrastructure");
    }

    @Test
    void sseEmitterRemainsAtWebBoundaryAndRawMarketEndpointsStayPresent() throws IOException {
        String controller = readRequired(APP_MARKET_WEB.resolve("MarketController.java"));

        assertTrue(controller.contains("/{symbol}/stream"), "raw market summary SSE endpoint must remain present");
        assertTrue(controller.contains("/{symbol}/candles/stream"), "raw candle SSE endpoint must remain present");

        try (Stream<Path> files = Stream.of(MAIN, APP_MAIN, CORE_MAIN).flatMap(root -> {
                try {
                    return Files.walk(root);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            })) {
            List<Path> offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("SseEmitter") && !isWebBoundary(path);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .toList();

            assertTrue(offenders.isEmpty(), "SseEmitter must stay in web/common-web boundary: " + offenders);
        }
    }

    private static String readRequired(Path path) throws IOException {
        assertTrue(Files.exists(path), () -> "Expected source file to exist: " + path);
        return Files.readString(path);
    }

    private static Path findRequired(String fileName) throws IOException {
        try (Stream<Path> files = Stream.of(MAIN, APP_MAIN, CORE_MAIN).flatMap(root -> {
            try {
                return Files.walk(root);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        })) {
            return files
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected source file to exist: " + fileName));
        }
    }

    private static boolean isWebBoundary(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/web/") || normalized.contains("/common/web/");
    }
}
