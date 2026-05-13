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
    void streamPassesExplicitClientKeyToGateway() {
        Providers providers = providers(1L);
        TradingExecutionStreamGateway gateway = mock(TradingExecutionStreamGateway.class);
        TradingExecutionStreamGateway.SubscriptionPermit permit =
                mock(TradingExecutionStreamGateway.SubscriptionPermit.class);
        when(gateway.reserve(1L, " tab-1 ")).thenReturn(permit);
        OrderController controller = controller(gateway, providers);

        controller.stream(" tab-1 ");

        verify(gateway).reserve(1L, " tab-1 ");
        verify(gateway).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void streamPassesMissingClientKeyToGateway() {
        Providers providers = providers(1L);
        TradingExecutionStreamGateway gateway = mock(TradingExecutionStreamGateway.class);
        TradingExecutionStreamGateway.SubscriptionPermit permit =
                mock(TradingExecutionStreamGateway.SubscriptionPermit.class);
        when(gateway.reserve(1L, null)).thenReturn(permit);
        OrderController controller = controller(gateway, providers);

        controller.stream(null);

        verify(gateway).reserve(1L, null);
        verify(gateway).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void streamReleasesPermitIfRegistrationFails() {
        Providers providers = providers(1L);
        TradingExecutionStreamGateway gateway = mock(TradingExecutionStreamGateway.class);
        TradingExecutionStreamGateway.SubscriptionPermit permit =
                mock(TradingExecutionStreamGateway.SubscriptionPermit.class);
        when(gateway.reserve(1L, "tab-1")).thenReturn(permit);
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(gateway).register(eq(permit), any(SseEmitter.class));
        OrderController controller = controller(gateway, providers);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> controller.stream("tab-1"));

        verify(gateway).release(permit);
    }

    @Test
    void streamDoesNotRegisterWhenReserveFails() {
        Providers providers = providers(1L);
        TradingExecutionStreamGateway gateway = mock(TradingExecutionStreamGateway.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("limit"))
                .when(gateway).reserve(1L, "tab-1");
        OrderController controller = controller(gateway, providers);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> controller.stream("tab-1"));

        verify(gateway, never()).register(any(), any());
    }

    private static OrderController controller(TradingExecutionStreamGateway gateway, Providers providers) {
        return new OrderController(
                mock(CreateOrderService.class),
                mock(GetOpenOrdersService.class),
                mock(GetOrderHistoryService.class),
                mock(CancelOrderService.class),
                mock(ModifyOrderService.class),
                gateway,
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
