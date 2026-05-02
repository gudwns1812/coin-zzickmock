package coin.coinzzickmock.feature.activity.api;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ActivityWebConfiguration implements WebMvcConfigurer {
    private final AuthenticatedActivityInterceptor authenticatedActivityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticatedActivityInterceptor)
                .addPathPatterns(
                        "/api/futures/account/**",
                        "/api/futures/positions/**",
                        "/api/futures/orders/**",
                        "/api/futures/rewards/**",
                        "/api/futures/shop/**",
                        "/api/futures/admin/**"
                );
    }
}
