package coin.coinzzickmock.feature.push.web;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PushCorsConfiguration implements WebMvcConfigurer {
    private final String[] allowedOriginPatterns;

    public PushCorsConfiguration(
            @Value("${coin.web.cors.allowed-origin-patterns}") String allowedOriginPatterns
    ) {
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/futures/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
