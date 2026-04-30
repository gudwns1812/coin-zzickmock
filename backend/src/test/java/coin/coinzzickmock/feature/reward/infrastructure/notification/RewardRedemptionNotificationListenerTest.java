package coin.coinzzickmock.feature.reward.infrastructure.notification;

import coin.coinzzickmock.feature.reward.application.event.RewardRedemptionCreatedEvent;
import coin.coinzzickmock.feature.reward.application.notification.RewardRedemptionNotifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardRedemptionNotificationListenerTest {
    @Test
    void delegatesCreatedEventsToNotifier() {
        CapturingNotifier notifier = new CapturingNotifier(false);
        RewardRedemptionNotificationListener listener = new RewardRedemptionNotificationListener(notifier);
        RewardRedemptionCreatedEvent event = event();

        listener.on(event);

        assertEquals(event, notifier.lastEvent);
    }

    @Test
    void notificationFailureDoesNotEscapeListener() {
        RewardRedemptionNotificationListener listener = new RewardRedemptionNotificationListener(new CapturingNotifier(true));

        assertDoesNotThrow(() -> listener.on(event()));
    }

    private RewardRedemptionCreatedEvent event() {
        return new RewardRedemptionCreatedEvent(
                "request-1",
                1L,
                "voucher.coffee",
                "커피 교환권",
                100,
                "010-1234-5678"
        );
    }

    private static class CapturingNotifier implements RewardRedemptionNotifier {
        private final boolean fail;
        private RewardRedemptionCreatedEvent lastEvent;

        private CapturingNotifier(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void notifyCreated(RewardRedemptionCreatedEvent event) {
            if (fail) {
                throw new IllegalStateException("smtp unavailable");
            }
            lastEvent = event;
        }
    }
}
