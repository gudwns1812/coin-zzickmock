package coin.coinzzickmock.feature.member.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.service.AuthenticateMemberService;
import coin.coinzzickmock.feature.member.application.service.CheckMemberAvailabilityService;
import coin.coinzzickmock.feature.member.application.service.GetMemberCredentialService;
import coin.coinzzickmock.feature.member.application.service.RegisterMemberService;
import coin.coinzzickmock.feature.member.application.service.WithdrawMemberService;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.infrastructure.security.JwtAccessTokenManager;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/futures/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegisterMemberService registerMemberService;
    private final AuthenticateMemberService authenticateMemberService;
    private final CheckMemberAvailabilityService checkMemberAvailabilityService;
    private final GetMemberCredentialService getMemberCredentialService;
    private final WithdrawMemberService withdrawMemberService;
    private final JwtAccessTokenManager jwtAccessTokenManager;
    private final Providers providers;

    @PostMapping("/register")
    public ApiResponse<AuthUserResponse> register(@RequestBody RegisterMemberRequest request) {
        if (request.address() == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "주소 정보는 필수입니다.");
        }

        registerMemberService.register(
                request.account(),
                request.password(),
                request.name(),
                request.email(),
                request.phoneNumber(),
                request.address().zipcode(),
                request.address().address(),
                request.address().addressDetail()
        );

        return ApiResponse.success(new AuthUserResponse(request.account().trim(), request.name().trim()));
    }

    @PostMapping("/duplicate")
    public ApiResponse<AccountAvailabilityResponse> duplicate(@RequestBody DuplicateAccountRequest request) {
        boolean available = checkMemberAvailabilityService.isAvailable(request.account());
        if (!available) {
            throw new CoreException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }
        return ApiResponse.success(new AccountAvailabilityResponse(true));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthUserResponse>> login(@RequestBody LoginRequest request) {
        MemberCredential memberCredential = authenticateMemberService.authenticate(request.account(), request.password());
        return withAccessToken(ApiResponse.success(new AuthUserResponse(
                memberCredential.memberId(),
                memberCredential.memberName()
        )), memberCredential);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtAccessTokenManager.expireAccessTokenCookie().toString())
                .body(ApiResponse.success(null));
    }

    @GetMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthUserResponse>> refresh() {
        Actor actor = providers.auth().currentActor();
        MemberCredential memberCredential = getMemberCredentialService.get(actor.memberId());
        return withAccessToken(ApiResponse.success(new AuthUserResponse(
                memberCredential.memberId(),
                memberCredential.memberName()
        )), memberCredential);
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(@RequestBody WithdrawMemberRequest request) {
        Actor actor = providers.auth().currentActor();
        if (request.memberId() == null || !actor.memberId().equals(request.memberId().trim())) {
            throw new CoreException(ErrorCode.FORBIDDEN, "본인 계정만 탈퇴할 수 있습니다.");
        }

        withdrawMemberService.withdraw(actor.memberId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtAccessTokenManager.expireAccessTokenCookie().toString())
                .body(ApiResponse.success(null));
    }

    private ResponseEntity<ApiResponse<AuthUserResponse>> withAccessToken(
            ApiResponse<AuthUserResponse> body,
            MemberCredential memberCredential
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtAccessTokenManager.buildAccessTokenCookie(
                        jwtAccessTokenManager.issue(memberCredential)
                ).toString())
                .body(body);
    }
}
