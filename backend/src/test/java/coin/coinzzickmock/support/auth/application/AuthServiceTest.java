package coin.coinzzickmock.support.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import coin.coinzzickmock.core.application.member.MemberInvestService;
import coin.coinzzickmock.core.domain.member.Address;
import coin.coinzzickmock.core.domain.member.Member;
import coin.coinzzickmock.core.domain.member.MemberAccount;
import coin.coinzzickmock.core.domain.member.MemberProfile;
import coin.coinzzickmock.storage.db.member.entity.MemberEntity;
import coin.coinzzickmock.storage.db.member.repository.MemberJpaRepository;
import coin.coinzzickmock.support.auth.application.result.AuthTokens;
import coin.coinzzickmock.support.auth.security.JwtTokenProvider;
import coin.coinzzickmock.support.error.AuthErrorType;
import coin.coinzzickmock.support.error.CoreException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberJpaRepository memberJpaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthProcessor authProcessor;

    @Mock
    private MemberInvestService memberInvestService;

    @InjectMocks
    private AuthService authService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .memberId("member-1")
                .account(MemberAccount.of("tester", "encoded-password"))
                .profile(MemberProfile.of("테스터", "tester@example.com", "010-1234-5678"))
                .address(Address.of("12345", "서울시 강남구", "101동"))
                .invest(0)
                .refreshTokenVersion(0L)
                .build();
    }

    @Test
    void registerEncodesPasswordAndSavesMember() {
        when(memberJpaRepository.existsByAccount("tester")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        authService.register(
                "tester",
                "plain-password",
                "테스터",
                "010-1234-5678",
                "tester@example.com",
                "12345",
                "서울시 강남구",
                "101동"
        );

        ArgumentCaptor<MemberEntity> captor = ArgumentCaptor.forClass(MemberEntity.class);
        verify(memberJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getAccount()).isEqualTo("tester");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void registerRejectsDuplicateAccount() {
        when(memberJpaRepository.existsByAccount("tester")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                "tester",
                "plain-password",
                "테스터",
                "010-1234-5678",
                "tester@example.com",
                "12345",
                "서울시 강남구",
                null
        ))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.DUPLICATE_ACCOUNT);
    }

    @Test
    void loginRejectsInvalidPassword() {
        when(memberJpaRepository.findByAccount("tester")).thenReturn(Optional.of(toEntity(member)));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("tester", "wrong-password"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.INVALID_CREDENTIALS);
    }

    @Test
    void updateInvestRejectsAnotherMembersRequest() {
        when(memberInvestService.updateInvest("member-1", "member-2", 4))
                .thenThrow(new CoreException(AuthErrorType.MEMBER_ACCESS_DENIED));

        assertThatThrownBy(
                () -> authService.updateInvest("member-1", "member-2", 4))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.MEMBER_ACCESS_DENIED);
    }

    @Test
    void loginReturnsAuthTokens() {
        when(memberJpaRepository.findByAccount("tester")).thenReturn(Optional.of(toEntity(member)));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(any(Member.class))).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(any(Member.class))).thenReturn("refresh-token");

        AuthTokens tokens = authService.login("tester", "plain-password");

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void updateInvestReturnsNewAccessToken() {
        when(memberInvestService.updateInvest("member-1", "member-1", 4)).thenReturn(member);
        when(jwtTokenProvider.createAccessToken(member)).thenReturn("renewed-access-token");

        String accessToken = authService.updateInvest("member-1", "member-1", 4);

        assertThat(accessToken).isEqualTo("renewed-access-token");
    }

    @Test
    void duplicateAccountValidationThrowsWhenDuplicated() {
        when(memberJpaRepository.existsByAccount("tester")).thenReturn(true);

        assertThatThrownBy(() -> authService.validateAccountAvailable("tester"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.DUPLICATE_ACCOUNT);
    }

    @Test
    void refreshRotatesRefreshTokenVersionAndReturnsNewTokens() {
        Claims claims = Mockito.mock(Claims.class);
        when(jwtTokenProvider.parseRefreshToken("refresh-token")).thenReturn(claims);
        when(claims.get("memberId", String.class)).thenReturn("member-1");
        when(claims.get("version", Long.class)).thenReturn(0L);

        Member updatedMember = Member.builder()
                .memberId("member-1")
                .account(MemberAccount.of("tester", "encoded-password"))
                .profile(MemberProfile.of("테스터", "tester@example.com", "010-1234-5678"))
                .address(Address.of("12345", "서울시 강남구", "101동"))
                .invest(0)
                .refreshTokenVersion(1L)
                .build();

        when(authProcessor.updateRefreshVersion("member-1", 0L)).thenReturn(updatedMember);
        when(jwtTokenProvider.createAccessToken(any(Member.class))).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(any(Member.class))).thenReturn("new-refresh-token");

        AuthTokens tokens = authService.refresh("refresh-token");

        assertThat(tokens.accessToken()).isEqualTo("new-access-token");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(updatedMember.getMemberId()).isEqualTo("member-1");
        assertThat(updatedMember.getRefreshTokenVersion()).isEqualTo(1L);
    }

    private MemberEntity toEntity(Member domain) {
        return MemberEntity.builder()
                .memberId(domain.getMemberId())
                .account(domain.getAccount().getAccount())
                .passwordHash(domain.getAccount().getPasswordHash())
                .name(domain.getProfile().getName())
                .email(domain.getProfile().getEmail())
                .phoneNumber(domain.getProfile().getPhoneNumber())
                .address(coin.coinzzickmock.storage.db.Address.builder()
                        .zipCode(domain.getAddress().getZipCode())
                        .address(domain.getAddress().getAddress())
                        .addressDetail(domain.getAddress().getAddressDetail())
                        .build())
                .invest(domain.getInvest())
                .refreshTokenVersion(domain.getRefreshTokenVersion())
                .build();
    }
}
