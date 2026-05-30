package coin.coinzzickmock.common.web.security;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.ErrorResponse;
import coin.coinzzickmock.common.web.FuturesUnsafeMethodOriginFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class FuturesApiSecurityConfiguration {
    private final FuturesUnsafeMethodOriginFilter unsafeMethodOriginFilter;
    private final FuturesCookieBearerTokenResolver cookieBearerTokenResolver;
    private final FuturesJwtAuthenticationConverter jwtAuthenticationConverter;
    private final ObjectMapper objectMapper;
    private final CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final AuthenticationSuccessHandler googleOAuthLoginSuccessHandler;
    private final AuthenticationFailureHandler googleOAuthLoginFailureHandler;

    @Bean
    FilterRegistrationBean<FuturesUnsafeMethodOriginFilter> futuresUnsafeMethodOriginFilterRegistration(
            FuturesUnsafeMethodOriginFilter filter
    ) {
        FilterRegistrationBean<FuturesUnsafeMethodOriginFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(1)
    SecurityFilterChain futuresPreflightSecurityFilterChain(HttpSecurity http) throws Exception {
        return statelessApi(http)
                .securityMatcher(futuresApiMatcher(HttpMethod.OPTIONS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    @ConditionalOnBean(ClientRegistrationRepository.class)
    SecurityFilterChain googleOAuthSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(new OrRequestMatcher(
                        pathPrefixMatcher("/oauth2/"),
                        pathPrefixMatcher("/login/oauth2/")
                ))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(authorizationRequestRepository))
                        .successHandler(googleOAuthLoginSuccessHandler)
                        .failureHandler(googleOAuthLoginFailureHandler))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain futuresAuthRecoverySecurityFilterChain(HttpSecurity http) throws Exception {
        return statelessApi(http)
                .securityMatcher(new OrRequestMatcher(
                        exactPathMatcher(HttpMethod.POST, "/api/futures/auth/register"),
                        exactPathMatcher(HttpMethod.POST, "/api/futures/auth/duplicate"),
                        exactPathMatcher(HttpMethod.POST, "/api/futures/auth/login"),
                        exactPathMatcher(HttpMethod.POST, "/api/futures/auth/logout"),
                        exactPathMatcher(HttpMethod.GET, "/api/futures/auth/google/onboarding"),
                        exactPathMatcher(HttpMethod.POST, "/api/futures/auth/google/link"),
                        exactPathMatcher(HttpMethod.POST, "/api/futures/auth/google/signup")
                ))
                .addFilterBefore(unsafeMethodOriginFilter, BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain futuresApiSecurityFilterChain(HttpSecurity http) throws Exception {
        return statelessApi(http)
                .securityMatcher("/api/futures/**")
                .addFilterBefore(unsafeMethodOriginFilter, BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(cookieBearerTokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET,
                                "/api/futures/markets",
                                "/api/futures/markets/**",
                                "/api/futures/leaderboard",
                                "/api/futures/leaderboard/search"
                        ).permitAll()
                        .requestMatchers("/api/futures/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .build();
    }

    private HttpSecurity statelessApi(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeSecurityError(response, ErrorCode.UNAUTHORIZED);
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeSecurityError(response, ErrorCode.FORBIDDEN);
    }

    private void writeSecurityError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.httpStatusCode());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(errorCode.name(), errorCode.message()));
    }

    private RequestMatcher futuresApiMatcher(HttpMethod method) {
        return request -> method.matches(request.getMethod()) && isFuturesApiPath(requestPath(request));
    }

    private RequestMatcher exactPathMatcher(HttpMethod method, String path) {
        return request -> method.matches(request.getMethod()) && path.equals(requestPath(request));
    }

    private RequestMatcher pathPrefixMatcher(String prefix) {
        return request -> requestPath(request).startsWith(prefix);
    }

    private boolean isFuturesApiPath(String path) {
        return "/api/futures".equals(path) || path.startsWith("/api/futures/");
    }

    private String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
