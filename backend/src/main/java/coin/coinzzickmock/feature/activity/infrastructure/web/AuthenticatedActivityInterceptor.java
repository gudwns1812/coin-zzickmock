package coin.coinzzickmock.feature.activity.infrastructure.web;

import coin.coinzzickmock.feature.activity.application.service.RecordMemberActivityService;
import coin.coinzzickmock.feature.activity.domain.ActivitySource;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticatedActivityInterceptor implements HandlerInterceptor {
    private final Providers providers;
    private final RecordMemberActivityService recordMemberActivityService;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        if (response.getStatus() >= 400) {
            return;
        }

        try {
            if (!providers.auth().isAuthenticated()) {
                return;
            }
            Actor actor = providers.auth().currentActor();
            recordMemberActivityService.record(actor.memberId(), ActivitySource.AUTHENTICATED_API);
        } catch (RuntimeException exception) {
            log.warn("Failed to collect authenticated activity.", exception);
        }
    }
}
