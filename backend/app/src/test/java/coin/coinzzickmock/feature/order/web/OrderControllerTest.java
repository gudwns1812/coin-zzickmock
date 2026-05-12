package coin.coinzzickmock.feature.order.web;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.order.application.service.CancelOrderService;
import coin.coinzzickmock.feature.order.application.service.CreateOrderService;
import coin.coinzzickmock.feature.order.application.service.GetOpenOrdersService;
import coin.coinzzickmock.feature.order.application.service.GetOrderHistoryService;
import coin.coinzzickmock.feature.order.application.service.ModifyOrderService;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class OrderControllerTest {
    @Test
    void streamAcceptsExplicitClientKeyAndPassesNormalizedValueToBroker() {
        Providers providers = providers(1L);
        TradingExecutionSseBroker broker = mock(TradingExecutionSseBroker.class);
        TradingExecutionSseBroker.SseSubscriptionPermit permit = mock(TradingExecutionSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(1L, "tab-1")).thenReturn(permit);
        OrderController controller = controller(broker, providers);

        controller.stream(" tab-1 ");

        verify(broker).reserve(1L, "tab-1");
        verify(broker).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void streamUsesFallbackClientKeyWhenMissing() {
        Providers providers = providers(1L);
        TradingExecutionSseBroker broker = mock(TradingExecutionSseBroker.class);
        TradingExecutionSseBroker.SseSubscriptionPermit permit = mock(TradingExecutionSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(eq(1L), org.mockito.ArgumentMatchers.startsWith("server:"))).thenReturn(permit);
        OrderController controller = controller(broker, providers);

        controller.stream(null);

        verify(broker).reserve(eq(1L), org.mockito.ArgumentMatchers.startsWith("server:"));
        verify(broker).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void streamReleasesPermitIfRegistrationFails() {
        Providers providers = providers(1L);
        TradingExecutionSseBroker broker = mock(TradingExecutionSseBroker.class);
        TradingExecutionSseBroker.SseSubscriptionPermit permit = mock(TradingExecutionSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(1L, "tab-1")).thenReturn(permit);
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(broker).register(eq(permit), any(SseEmitter.class));
        OrderController controller = controller(broker, providers);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> controller.stream("tab-1"));

        verify(broker).release(permit);
    }

    @Test
    void streamDoesNotRegisterWhenReserveFails() {
        Providers providers = providers(1L);
        TradingExecutionSseBroker broker = mock(TradingExecutionSseBroker.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("limit"))
                .when(broker).reserve(1L, "tab-1");
        OrderController controller = controller(broker, providers);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> controller.stream("tab-1"));

        verify(broker, never()).register(any(), any());
    }

    private static OrderController controller(TradingExecutionSseBroker broker, Providers providers) {
        return new OrderController(
                mock(CreateOrderService.class),
                mock(GetOpenOrdersService.class),
                mock(GetOrderHistoryService.class),
                mock(CancelOrderService.class),
                mock(ModifyOrderService.class),
                broker,
                providers,
                30_000L
        );
    }

    private static Providers providers(Long memberId) {
        Providers providers = mock(Providers.class);
        AuthProvider authProvider = mock(AuthProvider.class);
        when(authProvider.currentActor()).thenReturn(new Actor(memberId, "user", "user@example.com", "User"));
        when(providers.auth()).thenReturn(authProvider);
        return providers;
    }
}
