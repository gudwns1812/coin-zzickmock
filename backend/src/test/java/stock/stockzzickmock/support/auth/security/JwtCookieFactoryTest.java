package stock.stockzzickmock.support.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class JwtCookieFactoryTest {

    private JwtCookieFactory jwtCookieFactory;

    @BeforeEach
    void setUp() {
        jwtCookieFactory = new JwtCookieFactory(
                new JwtProperties(
                        "test-secret-test-secret-test-secret-test-secret",
                        3600,
                        1209600,
                        false
                )
        );
    }

    @Test
    void createAccessCookieUsesExpectedAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseCookie cookie = jwtCookieFactory.createAccessTokenCookie("access-token", request);

        assertThat(cookie.getName()).isEqualTo("accessToken");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(3600);
    }

    @Test
    void expireRefreshCookieSetsZeroMaxAge() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseCookie cookie = jwtCookieFactory.expireRefreshTokenCookie(request);

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
    }
}
