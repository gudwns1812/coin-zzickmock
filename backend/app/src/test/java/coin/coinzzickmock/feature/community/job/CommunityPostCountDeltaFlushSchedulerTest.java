package coin.coinzzickmock.feature.community.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import coin.coinzzickmock.feature.community.application.service.FlushCommunityPostCountDeltasService;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class CommunityPostCountDeltaFlushSchedulerTest {
    @Test
    void flushRunsEverySecondByDefault() throws NoSuchMethodException {
        Method method = CommunityPostCountDeltaFlushScheduler.class.getDeclaredMethod("flush");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("${coin.community.count-delta.flush-delay-ms:1000}", scheduled.fixedDelayString());
        assertEquals("${coin.community.count-delta.initial-delay-ms:1000}", scheduled.initialDelayString());
    }

    @Test
    void delegatesFlushToApplicationService() {
        FlushCommunityPostCountDeltasService service = mock(FlushCommunityPostCountDeltasService.class);
        CommunityPostCountDeltaFlushScheduler scheduler = new CommunityPostCountDeltaFlushScheduler(service);

        scheduler.flush();

        verify(service).flush();
    }
}
