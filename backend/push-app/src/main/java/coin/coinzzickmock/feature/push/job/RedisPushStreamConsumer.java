package coin.coinzzickmock.feature.push.job;

import coin.coinzzickmock.feature.push.application.RedisPushStreamReader;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPushStreamConsumer {
    private final RedisPushStreamReader streamReader;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeGroups() {
        streamReader.initializeGroups();
    }

    @Scheduled(fixedDelayString = "${coin.push.server.poll-fixed-delay-ms:100}")
    public void poll() {
        streamReader.poll();
    }
}
