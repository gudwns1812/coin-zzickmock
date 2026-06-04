package coin.coinzzickmock.feature.community.job;

import coin.coinzzickmock.feature.community.application.service.FlushCommunityPostCountDeltasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityPostCountDeltaFlushScheduler {
    private final FlushCommunityPostCountDeltasService flushCommunityPostCountDeltasService;

    @Scheduled(
            fixedDelayString = "${coin.community.count-delta.flush-delay-ms:1000}",
            initialDelayString = "${coin.community.count-delta.initial-delay-ms:1000}"
    )
    public void flush() {
        try {
            flushCommunityPostCountDeltasService.flush();
        } catch (RuntimeException exception) {
            log.warn("Failed to flush community post count deltas.", exception);
        }
    }
}
