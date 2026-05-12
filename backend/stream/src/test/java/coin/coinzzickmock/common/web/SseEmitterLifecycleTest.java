package coin.coinzzickmock.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterLifecycleTest {
    @Test
    void timeoutCallbackCompletesEmitterAfterReleaseCallback() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        StringBuilder calls = new StringBuilder();

        SseEmitterLifecycle.bind(
                emitter,
                () -> calls.append("complete"),
                () -> calls.append("timeout"),
                error -> calls.append("error")
        );

        emitter.fireTimeout();

        assertThat(calls).hasToString("timeout");
        assertThat(emitter.completed).isTrue();
    }

    @Test
    void completeSilentlyIgnoresAlreadyClosedEmitter() {
        SseEmitterLifecycle.completeSilently(new CompletionFailingSseEmitter());
    }

    private static final class CapturingSseEmitter extends SseEmitter {
        private Runnable timeoutCallback;
        private boolean completed;

        private CapturingSseEmitter() {
            super(0L);
        }

        @Override
        public synchronized void onCompletion(Runnable callback) {
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            this.timeoutCallback = callback;
        }

        @Override
        public synchronized void onError(Consumer<Throwable> callback) {
        }

        @Override
        public synchronized void complete() {
            this.completed = true;
        }

        private void fireTimeout() {
            timeoutCallback.run();
        }
    }

    private static final class CompletionFailingSseEmitter extends SseEmitter {
        private CompletionFailingSseEmitter() {
            super(0L);
        }

        @Override
        public synchronized void complete() {
            throw new IllegalStateException("already closed");
        }
    }
}
