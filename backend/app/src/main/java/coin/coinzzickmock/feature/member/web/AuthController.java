package coin.coinzzickmock.feature.member.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.activity.application.service.RecordMemberActivityService;
import coin.coinzzickmock.feature.activity.domain.ActivitySource;
import coin.coinzzickmock.feature.member.application.result.MemberProfileResult;
import coin.coinzzickmock.feature.member.application.service.AuthenticateMemberService;
import coin.coinzzickmock.feature.member.application.service.CheckMemberAvailabilityService;
import coin.coinzzickmock.feature.member.application.service.GetMemberProfileService;
import coin.coinzzickmock.feature.member.application.service.RegisterMemberService;
import coin.coinzzickmock.feature.member.application.service.WithdrawMemberService;
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
    private final GetMemberProfileService getMemberProfileService;
    private final WithdrawMemberService withdrawMemberService;
    private final AccessTokenCookieFactory accessTokenCookieFactory;
    private final Providers providers;
    private final RecordMemberActivityService recordMemberActivityService;

    @PostMapping("/register")
    public ApiResponse<AuthUserResponse> register(@RequestBody RegisterMemberRequest request) {
        if (request.address() == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        MemberProfileResult memberProfile = registerMemberService.register(
                request.account(),
                request.password(),
                request.name(),
                request.nickname(),
                request.email(),
                request.phoneNumber(),
                request.address().zipcode(),
                request.address().address(),
                request.address().addressDetail()
        );

        return ApiResponse.success(AuthUserResponse.from(memberProfile));
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
        MemberProfileResult memberProfile = authenticateMemberService.authenticate(request.account(), request.password());
        recordMemberActivityService.record(memberProfile.memberId(), ActivitySource.LOGIN);
        return withAccessToken(ApiResponse.success(AuthUserResponse.from(memberProfile)), memberProfile);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookieFactory.expire().toString())
                .body(ApiResponse.success(null));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me() {
        Actor actor = providers.auth().currentActor();
        MemberProfileResult memberProfile = getMemberProfileService.get(actor.memberId());
        return ApiResponse.success(AuthUserResponse.from(memberProfile));
    }

    @GetMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthUserResponse>> refresh() {
        Actor actor = providers.auth().currentActor();
        MemberProfileResult memberProfile = getMemberProfileService.get(actor.memberId());
        return withAccessToken(ApiResponse.success(AuthUserResponse.from(memberProfile)), memberProfile);
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(@RequestBody WithdrawMemberRequest request) {
        Actor actor = providers.auth().currentActor();
        withdrawMemberService.withdraw(actor.memberId(), request.memberId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookieFactory.expire().toString())
                .body(ApiResponse.success(null));
    }

    private ResponseEntity<ApiResponse<AuthUserResponse>> withAccessToken(
            ApiResponse<AuthUserResponse> body,
            MemberProfileResult memberProfile
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookieFactory.issue(memberProfile).toString())
                .body(body);
    }
}
