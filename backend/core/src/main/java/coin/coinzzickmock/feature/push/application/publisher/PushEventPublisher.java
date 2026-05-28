package coin.coinzzickmock.feature.push.application.publisher;

import coin.coinzzickmock.feature.push.application.dto.PushEventEnvelope;

public interface PushEventPublisher {
    void publish(PushEventEnvelope event);
}
