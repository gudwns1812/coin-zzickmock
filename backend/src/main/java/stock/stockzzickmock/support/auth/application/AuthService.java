package stock.stockzzickmock.support.auth.application;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stock.stockzzickmock.core.domain.member.Member;
import stock.stockzzickmock.storage.db.member.entity.MemberEntity;
import stock.stockzzickmock.storage.db.member.repository.MemberJpaRepository;
import stock.stockzzickmock.support.error.AuthErrorType;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.auth.api.dto.request.InvestRequest;
import stock.stockzzickmock.support.auth.api.dto.request.LoginRequest;
import stock.stockzzickmock.support.auth.api.dto.request.RegisterRequest;
import stock.stockzzickmock.support.auth.api.dto.request.WithdrawRequest;
import stock.stockzzickmock.support.auth.security.AuthenticatedMember;
import stock.stockzzickmock.support.auth.security.JwtCookieFactory;
import stock.stockzzickmock.support.auth.security.JwtTokenProvider;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberJpaRepository memberJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtCookieFactory jwtCookieFactory;

    @Transactional
    public void register(RegisterRequest request) {
        if (memberJpaRepository.existsByAccount(request.account())) {
            throw new CoreException(AuthErrorType.DUPLICATE_ACCOUNT);
        }

        Member member = Member.create(
                request.account(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.email(),
                request.phoneNumber(),
                request.address().zipcode(),
                request.address().address(),
                request.address().addressDetail() == null ? "" : request.address().addressDetail()
        );
        memberJpaRepository.save(toEntity(member));
    }

    public boolean isAccountAvailable(String account) {
        return !memberJpaRepository.existsByAccount(account);
    }

    public List<String> login(LoginRequest request, HttpServletRequest httpServletRequest) {
        Member member = memberJpaRepository.findByAccount(request.account())
                .map(MemberEntity::toDomain)
                .orElseThrow(() -> new CoreException(AuthErrorType.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new CoreException(AuthErrorType.INVALID_CREDENTIALS);
        }

        return issueTokenCookies(member, httpServletRequest, true);
    }

    public List<String> refresh(String refreshToken, HttpServletRequest request) {
        Claims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        String memberId = claims.get("memberId", String.class);
        Long version = claims.get("version", Long.class);

        Member member = memberJpaRepository.findById(memberId)
                .map(MemberEntity::toDomain)
                .orElseThrow(() -> new CoreException(AuthErrorType.MEMBER_NOT_FOUND));

        if (!member.getRefreshTokenVersion().equals(version)) {
            throw new CoreException(AuthErrorType.INVALID_JWT);
        }

        return issueTokenCookies(member, request, true);
    }

    @Transactional
    public List<String> logout(AuthenticatedMember authenticatedMember, HttpServletRequest request) {
        Member member = getMember(authenticatedMember.memberId());
        member.updateRefreshTokenVersion();
        memberJpaRepository.save(toEntity(member));
        return expireCookies(request);
    }

    @Transactional
    public List<String> withdraw(
            AuthenticatedMember authenticatedMember,
            WithdrawRequest request,
            HttpServletRequest httpServletRequest
    ) {
        validateMemberOwnership(authenticatedMember.memberId(), request.memberId());
        Member member = getMember(authenticatedMember.memberId());
        memberJpaRepository.delete(toEntity(member));
        return expireCookies(httpServletRequest);
    }

    @Transactional
    public List<String> updateInvest(
            AuthenticatedMember authenticatedMember,
            InvestRequest request,
            HttpServletRequest httpServletRequest
    ) {
        validateMemberOwnership(authenticatedMember.memberId(), request.memberId());
        Member member = getMember(authenticatedMember.memberId());
        member.updateInvest(request.investScore());
        memberJpaRepository.save(toEntity(member));
        return List.of(cookieHeader(jwtCookieFactory.createAccessTokenCookie(
                jwtTokenProvider.createAccessToken(member),
                httpServletRequest
        )));
    }

    private Member getMember(String memberId) {
        return memberJpaRepository.findById(memberId)
                .map(MemberEntity::toDomain)
                .orElseThrow(() -> new CoreException(AuthErrorType.MEMBER_NOT_FOUND));
    }

    private List<String> issueTokenCookies(Member member, HttpServletRequest request, boolean issueRefresh) {
        ResponseCookie accessCookie = jwtCookieFactory.createAccessTokenCookie(
                jwtTokenProvider.createAccessToken(member),
                request
        );
        if (!issueRefresh) {
            return List.of(cookieHeader(accessCookie));
        }

        ResponseCookie refreshCookie = jwtCookieFactory.createRefreshTokenCookie(
                jwtTokenProvider.createRefreshToken(member),
                request
        );
        return List.of(cookieHeader(accessCookie), cookieHeader(refreshCookie));
    }

    private List<String> expireCookies(HttpServletRequest request) {
        return List.of(
                cookieHeader(jwtCookieFactory.expireAccessTokenCookie(request)),
                cookieHeader(jwtCookieFactory.expireRefreshTokenCookie(request))
        );
    }

    private String cookieHeader(ResponseCookie cookie) {
        return cookie.toString();
    }

    private void validateMemberOwnership(String authenticatedMemberId, String requestMemberId) {
        if (!authenticatedMemberId.equals(requestMemberId)) {
            throw new CoreException(AuthErrorType.MEMBER_ACCESS_DENIED);
        }
    }

    private MemberEntity toEntity(Member member) {
        return MemberEntity.builder()
                .memberId(member.getMemberId())
                .account(member.getAccount())
                .passwordHash(member.getPasswordHash())
                .name(member.getName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .zipCode(member.getZipCode())
                .address(member.getAddress())
                .addressDetail(member.getAddressDetail())
                .invest(member.getInvest())
                .refreshTokenVersion(member.getRefreshTokenVersion())
                .build();
    }
}
