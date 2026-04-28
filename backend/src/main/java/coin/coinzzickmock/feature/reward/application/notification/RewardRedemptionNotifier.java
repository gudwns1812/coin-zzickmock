package coin.coinzzickmock.feature.reward.application.notification;

import coin.coinzzickmock.feature.reward.application.event.RewardRedemptionCreatedEvent;

public interface RewardRedemptionNotifier {
    void notifyCreated(RewardRedemptionCreatedEvent event);
}
