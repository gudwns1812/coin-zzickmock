package stock.stockzzickmock.support.auth.application;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import stock.stockzzickmock.support.error.AuthErrorType;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.auth.api.dto.request.AddressRequest;
import stock.stockzzickmock.support.auth.api.dto.request.InvestRequest;
import stock.stockzzickmock.support.auth.api.dto.request.LoginRequest;
import stock.stockzzickmock.support.auth.api.dto.request.RegisterRequest;
import stock.stockzzickmock.core.domain.member.Member;
import stock.stockzzickmock.storage.db.member.MemberRepository;
import stock.stockzzickmock.support.auth.security.AuthenticatedMember;
import stock.stockzzickmock.support.auth.security.JwtCookieFactory;
import stock.stockzzickmock.support.auth.security.JwtTokenProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtCookieFactory jwtCookieFactory;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthService authService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .memberId("member-1")
                .account("tester")
                .passwordHash("encoded-password")
                .name("테스터")
                .email("tester@example.com")
                .phoneNumber("010-1234-5678")
                .zipCode("12345")
                .address("서울시 강남구")
                .addressDetail("101동")
                .invest(0)
                .refreshTokenVersion(0L)
                .build();
    }

    @Test
    void registerEncodesPasswordAndSavesMember() {
        RegisterRequest request = new RegisterRequest(
                "tester",
                "plain-password",
                "테스터",
                "010-1234-5678",
                "tester@example.com",
                new AddressRequest("12345", "서울시 강남구", "101동"),
                "ignored"
        );
        when(memberRepository.existsByAccount("tester")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        authService.register(request);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getAccount()).isEqualTo("tester");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void registerRejectsDuplicateAccount() {
        RegisterRequest request = new RegisterRequest(
                "tester",
                "plain-password",
                "테스터",
                "010-1234-5678",
                "tester@example.com",
                new AddressRequest("12345", "서울시 강남구", "101동"),
                null
        );
        when(memberRepository.existsByAccount("tester")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.DUPLICATE_ACCOUNT);
    }

    @Test
    void loginRejectsInvalidPassword() {
        when(memberRepository.findByAccount("tester")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("tester", "wrong-password"), httpServletRequest))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.INVALID_CREDENTIALS);
    }

    @Test
    void updateInvestRejectsAnotherMembersRequest() {
        InvestRequest request = new InvestRequest("member-2", 4);

        assertThatThrownBy(() -> authService.updateInvest(new AuthenticatedMember("member-1"), request, httpServletRequest))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.MEMBER_ACCESS_DENIED);
    }

    @Test
    void loginReturnsCookieHeaders() {
        when(memberRepository.findByAccount("tester")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(member)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(member)).thenReturn("refresh-token");
        when(jwtCookieFactory.createAccessTokenCookie(eq("access-token"), any(HttpServletRequest.class)))
                .thenReturn(ResponseCookie.from("accessToken", "access-token").path("/").build());
        when(jwtCookieFactory.createRefreshTokenCookie(eq("refresh-token"), any(HttpServletRequest.class)))
                .thenReturn(ResponseCookie.from("refreshToken", "refresh-token").path("/").build());

        var cookies = authService.login(new LoginRequest("tester", "plain-password"), httpServletRequest);

        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0)).contains("accessToken=access-token");
        assertThat(cookies.get(1)).contains("refreshToken=refresh-token");
    }
}
