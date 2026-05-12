package coin.coinzzickmock.common.web;

import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class SseEmitterLifecycle {
    private SseEmitterLifecycle() {
    }

    public static void bind(
            SseEmitter emitter,
            Runnable onCompletion,
            Runnable onTimeout,
            Consumer<Throwable> onError
    ) {
        emitter.onCompletion(onCompletion);
        emitter.onTimeout(() -> {
            onTimeout.run();
            completeSilently(emitter);
        });
        emitter.onError(onError);
    }

    public static void completeSilently(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (RuntimeException ignored) {
            // The client may already be gone.
        }
    }
}
