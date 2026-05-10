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
    private static final Path MARKET_WEB = MAIN.resolve("coin/coinzzickmock/feature/market/web");

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

        assertTrue(broker.contains("MarketSummaryUpdatedEvent"), "broker must fan out market summary updates");
        assertTrue(broker.contains("MarketCandleUpdatedEvent"), "broker must fan out candle updates");
        assertTrue(broker.contains("MarketHistoryFinalizedEvent"), "broker must fan out history-finalized updates");
        assertTrue(broker.contains("sessionsForSummary"), "summary fan-out must come from registry snapshot");
        assertTrue(broker.contains("sessionsForCandle"), "candle fan-out must come from registry snapshot");
        assertTrue(broker.contains("onCompletion"), "broker must wire completion lifecycle callback");
        assertTrue(broker.contains("onTimeout"), "broker must wire timeout lifecycle callback");
        assertTrue(broker.contains("onError"), "broker must wire error lifecycle callback");
        assertTrue(broker.contains("releaseSession"), "broker must release registry sessions on lifecycle/send failure");
        assertTrue(broker.contains("SseTelemetry"), "broker must preserve SSE telemetry boundary");
        assertTrue(broker.contains("MARKET_SUMMARY"), "unified envelopes must include market summary type");
        assertTrue(broker.contains("MARKET_CANDLE"), "unified envelopes must include market candle type");
        assertTrue(broker.contains("MARKET_HISTORY_FINALIZED"), "unified envelopes must include history finalized type");
        assertTrue(broker.contains("INITIAL_SNAPSHOT") || broker.contains("LIVE"),
                "envelopes must carry source metadata for initial/live de-duplication");
    }

    @Test
    void controllerAddsAuthenticatedUnifiedEndpointWithoutCouplingToPositionInternals() throws IOException {
        String controller = readRequired(MARKET_WEB.resolve("MarketController.java"));

        assertTrue(controller.contains("/stream"), "controller must expose unified /api/futures/markets/stream");
        assertTrue(controller.contains("MarketStreamBroker"), "controller must delegate unified stream lifecycle to broker");
        assertTrue(controller.contains("OpenPositionSymbolsReader"),
                "controller must ask a narrow application-facing reader for open position symbols");
        assertFalse(controller.contains("PositionRepository"), "market web must not depend on position repositories");
        assertFalse(controller.contains("PositionJpa"), "market web must not depend on position persistence internals");
        assertTrue(controller.contains("currentActor"), "unified endpoint must resolve the authenticated actor");
        assertTrue(controller.contains("clientKey"), "unified endpoint must be scoped by clientKey");
        assertTrue(controller.contains("interval"), "unified endpoint must register active candle interval");
    }

    @Test
    void positionMutationsPublishBusinessSemanticEventsOnly() throws IOException {
        Path opened = findRequired("PositionOpenedEvent.java");
        Path fullyClosed = findRequired("PositionFullyClosedEvent.java");
        String openedSource = Files.readString(opened);
        String closedSource = Files.readString(fullyClosed);
        String openApplier = readRequired(MAIN.resolve(
                "coin/coinzzickmock/feature/order/application/service/FilledOpenOrderApplier.java"));
        String closeFinalizer = readRequired(MAIN.resolve(
                "coin/coinzzickmock/feature/position/application/close/PositionCloseFinalizer.java"));

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
        assertTrue(openApplier.contains("AfterCommitEventPublisher") || closeFinalizer.contains("AfterCommitEventPublisher"),
                "position mutation events must be published through after-commit infrastructure");
    }

    @Test
    void sseEmitterRemainsAtWebBoundaryAndRawMarketEndpointsStayPresent() throws IOException {
        String controller = readRequired(MARKET_WEB.resolve("MarketController.java"));

        assertTrue(controller.contains("/{symbol}/stream"), "raw market summary SSE endpoint must remain present");
        assertTrue(controller.contains("/{symbol}/candles/stream"), "raw candle SSE endpoint must remain present");

        try (Stream<Path> files = Files.walk(MAIN)) {
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
        try (Stream<Path> files = Files.walk(MAIN)) {
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
