package stock.stockzzickmock.support.auth.application;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stock.stockzzickmock.core.domain.member.Member;
import stock.stockzzickmock.storage.db.Address;
import stock.stockzzickmock.storage.db.member.entity.MemberEntity;
import stock.stockzzickmock.storage.db.member.repository.MemberJpaRepository;
import stock.stockzzickmock.support.auth.application.result.AuthTokens;
import stock.stockzzickmock.support.auth.security.JwtTokenProvider;
import stock.stockzzickmock.support.error.AuthErrorType;
import stock.stockzzickmock.support.error.CoreException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberJpaRepository memberJpaRepository;
    private final AuthProcessor authProcessor;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void register(
            String account,
            String password,
            String name,
            String phoneNumber,
            String email,
            String zipcode,
            String address,
            String addressDetail
    ) {
        validateAccountAvailable(account);

        Member member = Member.create(
                account,
                passwordEncoder.encode(password),
                name,
                email,
                phoneNumber,
                zipcode,
                address,
                addressDetail == null ? "" : addressDetail
        );
        memberJpaRepository.save(toEntity(member));
    }

    public void validateAccountAvailable(String account) {
        if (memberJpaRepository.existsByAccount(account)) {
            throw new CoreException(AuthErrorType.DUPLICATE_ACCOUNT);
        }
    }

    public AuthTokens login(String account, String password) {
        Member member = memberJpaRepository.findByAccount(account)
                .map(MemberEntity::toDomain)
                .orElseThrow(() -> new CoreException(AuthErrorType.INVALID_CREDENTIALS));

        if (!member.matchPassword(passwordEncoder, password)) {
            throw new CoreException(AuthErrorType.INVALID_CREDENTIALS);
        }

        return issueTokens(member);
    }

    public AuthTokens refresh(String refreshToken) {
        Claims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        String memberId = claims.get("memberId", String.class);
        Long version = claims.get("version", Long.class);

        if (version == null) {
            throw new CoreException(AuthErrorType.INVALID_JWT);
        }

        Member member = authProcessor.updateRefreshVersion(memberId, version);

        return issueTokens(member);
    }

    @Transactional
    public void logout(String authenticatedMemberId) {
        Member member = getMember(authenticatedMemberId);
        member.updateRefreshTokenVersion();
        memberJpaRepository.save(toEntity(member));
    }

    @Transactional
    public void withdraw(String authenticatedMemberId, String requestMemberId) {
        validateMemberOwnership(authenticatedMemberId, requestMemberId);
        Member member = getMember(authenticatedMemberId);
        memberJpaRepository.deleteById(member.getMemberId());
    }

    public String updateInvest(String authenticatedMemberId, String requestMemberId, int investScore) {
        validateMemberOwnership(authenticatedMemberId, requestMemberId);
        Member member = authProcessor.updateInvest(authenticatedMemberId, investScore);

        return jwtTokenProvider.createAccessToken(member);
    }

    private Member getMember(String memberId) {
        return memberJpaRepository.findById(memberId)
                .map(MemberEntity::toDomain)
                .orElseThrow(() -> new CoreException(AuthErrorType.MEMBER_NOT_FOUND));
    }

    private AuthTokens issueTokens(Member member) {
        String accessToken = jwtTokenProvider.createAccessToken(member);

        return new AuthTokens(
                accessToken,
                jwtTokenProvider.createRefreshToken(member)
        );
    }

    private void validateMemberOwnership(String authenticatedMemberId, String requestMemberId) {
        if (!authenticatedMemberId.equals(requestMemberId)) {
            throw new CoreException(AuthErrorType.MEMBER_ACCESS_DENIED);
        }
    }

    private MemberEntity toEntity(Member member) {
        return MemberEntity.builder()
                .memberId(member.getMemberId())
                .account(member.getAccount().getAccount())
                .passwordHash(member.getAccount().getPasswordHash())
                .name(member.getProfile().getName())
                .email(member.getProfile().getEmail())
                .phoneNumber(member.getProfile().getPhoneNumber())
                .address(Address.builder()
                        .zipCode(member.getAddress().getZipCode())
                        .address(member.getAddress().getAddress())
                        .addressDetail(member.getAddress().getAddressDetail())
                        .build())
                .invest(member.getInvest())
                .refreshTokenVersion(member.getRefreshTokenVersion())
                .build();
    }
}
