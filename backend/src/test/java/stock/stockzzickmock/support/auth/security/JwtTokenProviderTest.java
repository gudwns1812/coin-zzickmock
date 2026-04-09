package stock.stockzzickmock.support.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import stock.stockzzickmock.core.domain.member.Address;
import stock.stockzzickmock.core.domain.member.Member;
import stock.stockzzickmock.core.domain.member.MemberAccount;
import stock.stockzzickmock.core.domain.member.MemberProfile;
import stock.stockzzickmock.support.error.AuthErrorType;
import stock.stockzzickmock.support.error.CoreException;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private Member member;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                new JwtProperties(
                        "test-secret-test-secret-test-secret-test-secret",
                        3600,
                        1209600,
                        false
                )
        );
        member = Member.builder()
                .memberId("member-1")
                .account(MemberAccount.of("tester", "encoded"))
                .profile(MemberProfile.of("테스터", "tester@example.com", "010-1234-5678"))
                .address(Address.of("12345", "서울시 강남구", "101동"))
                .invest(3)
                .refreshTokenVersion(7L)
                .build();
    }

    @Test
    void createAndParseAccessToken() {
        String token = jwtTokenProvider.createAccessToken(member);

        Claims claims = jwtTokenProvider.parseAccessToken(token);

        assertThat(claims.getSubject()).isEqualTo(JwtTokenType.ACCESS_TOKEN.name());
        assertThat(claims.get("memberId", String.class)).isEqualTo("member-1");
        assertThat(claims.get("memberName", String.class)).isEqualTo("테스터");
        assertThat(claims.get("Address", String.class)).isEqualTo("서울시 강남구");
        assertThat(claims.get("AddressDetail", String.class)).isEqualTo("101동");
        assertThat(claims.get("invest", Integer.class)).isEqualTo(3);
    }

    @Test
    void createAndParseRefreshToken() {
        String token = jwtTokenProvider.createRefreshToken(member);

        Claims claims = jwtTokenProvider.parseRefreshToken(token);

        assertThat(claims.getSubject()).isEqualTo(JwtTokenType.REFRESH_TOKEN.name());
        assertThat(claims.get("memberId", String.class)).isEqualTo("member-1");
        assertThat(claims.get("version", Long.class)).isEqualTo(7L);
    }

    @Test
    void refreshTokenCannotBeParsedAsAccessToken() {
        String refreshToken = jwtTokenProvider.createRefreshToken(member);

        assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(refreshToken))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.INVALID_JWT);
    }
}
