package coin.coinzzickmock.common.web.security;

import static org.springframework.security.config.Customizer.withDefaults;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
class PushSecurityConfiguration {
    private final PushCookieBearerTokenResolver cookieBearerTokenResolver;

    @Bean
    SecurityFilterChain pushSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/futures/**")
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(cookieBearerTokenResolver)
                        .jwt(withDefaults()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/api/futures/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/futures/stream/orders",
                                "/api/futures/orders/stream"
                        ).authenticated()
                        .anyRequest().permitAll())
                .build();
    }
}
