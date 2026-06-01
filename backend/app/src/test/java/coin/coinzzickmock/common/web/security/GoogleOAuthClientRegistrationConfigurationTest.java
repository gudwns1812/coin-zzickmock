package coin.coinzzickmock.common.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleOAuthClientRegistrationConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GoogleOAuthClientRegistrationConfiguration.class);

    @Test
    void createsGoogleClientRegistrationWhenCredentialsAreConfigured() {
        contextRunner
                .withPropertyValues(
                        "app.auth.google.client-id=google-client-id",
                        "app.auth.google.client-secret=google-client-secret"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ClientRegistrationRepository.class);

                    ClientRegistration registration = context.getBean(ClientRegistrationRepository.class)
                            .findByRegistrationId("google");

                    assertThat(registration).isNotNull();
                    assertThat(registration.getClientId()).isEqualTo("google-client-id");
                    assertThat(registration.getClientSecret()).isEqualTo("google-client-secret");
                    assertThat(registration.getRedirectUri())
                            .isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
                    assertThat(registration.getScopes()).contains("openid", "profile", "email");
                });
    }

    @Test
    void leavesOAuthClientRegistrationDisabledWhenCredentialsAreMissing() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class));
    }

    @Test
    void leavesOAuthClientRegistrationDisabledWhenASecretIsMissing() {
        contextRunner
                .withPropertyValues("app.auth.google.client-id=google-client-id")
                .run(context -> assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class));
    }
}
