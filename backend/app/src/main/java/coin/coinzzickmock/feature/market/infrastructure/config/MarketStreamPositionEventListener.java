package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.web.MarketStreamBroker;
import coin.coinzzickmock.feature.position.application.event.PositionFullyClosedEvent;
import coin.coinzzickmock.feature.position.application.event.PositionOpenedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketStreamPositionEventListener {
    private final MarketStreamBroker marketStreamBroker;

    @EventListener
    public void onPositionOpened(PositionOpenedEvent event) {
        marketStreamBroker.addOpenPositionReason(event.memberId(), event.symbol());
    }

    @EventListener
    public void onPositionFullyClosed(PositionFullyClosedEvent event) {
        marketStreamBroker.removeOpenPositionReason(event.memberId(), event.symbol());
    }
}
