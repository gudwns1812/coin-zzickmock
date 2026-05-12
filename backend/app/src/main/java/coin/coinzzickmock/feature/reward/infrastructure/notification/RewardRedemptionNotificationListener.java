package coin.coinzzickmock.feature.reward.infrastructure.notification;

import coin.coinzzickmock.feature.reward.application.event.RewardRedemptionCreatedEvent;
import coin.coinzzickmock.feature.reward.application.notification.RewardRedemptionNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RewardRedemptionNotificationListener {
    private final RewardRedemptionNotifier rewardRedemptionNotifier;

    @EventListener
    public void on(RewardRedemptionCreatedEvent event) {
        try {
            rewardRedemptionNotifier.notifyCreated(event);
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to send reward redemption notification. provider=smtp requestId={}",
                    event.requestId(),
                    exception
            );
        }
    }
}
