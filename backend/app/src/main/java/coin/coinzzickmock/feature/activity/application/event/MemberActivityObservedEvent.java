package coin.coinzzickmock.feature.activity.application.event;

import coin.coinzzickmock.feature.activity.domain.ActivitySource;
import java.time.Instant;

public record MemberActivityObservedEvent(
        Long memberId,
        ActivitySource source,
        Instant observedAt
) {
}
