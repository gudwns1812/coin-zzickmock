package coin.coinzzickmock.support.auth.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import coin.coinzzickmock.support.auth.api.dto.request.DuplicateAccountRequest;
import coin.coinzzickmock.support.auth.api.dto.request.InvestRequest;
import coin.coinzzickmock.support.auth.api.dto.request.LoginRequest;
import coin.coinzzickmock.support.auth.api.dto.request.RegisterRequest;
import coin.coinzzickmock.support.auth.api.dto.request.WithdrawRequest;
import coin.coinzzickmock.support.auth.application.AuthService;
import coin.coinzzickmock.support.auth.application.result.AuthTokens;
import coin.coinzzickmock.support.auth.security.AuthenticatedMember;
import coin.coinzzickmock.support.auth.security.JwtCookieFactory;
import coin.coinzzickmock.support.error.AuthErrorType;
import coin.coinzzickmock.support.error.CoreException;
import coin.coinzzickmock.support.response.ApiResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtCookieFactory jwtCookieFactory;

    public AuthController(AuthService authService, JwtCookieFactory jwtCookieFactory) {
        this.authService = authService;
        this.jwtCookieFactory = jwtCookieFactory;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(
                request.account(),
                request.password(),
                request.name(),
                request.phoneNumber(),
                request.email(),
                request.zipcode(),
                request.address(),
                request.addressDetail()
        );
        return ApiResponse.success();
    }

    @PostMapping("/duplicate")
    public ResponseEntity<ApiResponse<Boolean>> duplicate(@Valid @RequestBody DuplicateAccountRequest request) {
        authService.validateAccountAvailable(request.account());
        return ResponseEntity.ok(ApiResponse.success(true));
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        AuthTokens tokens = authService.login(request.account(), request.password());
        appendAuthCookies(tokens, httpServletRequest, httpServletResponse);
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.logout(authenticatedMember.memberId());
        expireAuthCookies(request, response);
        return ApiResponse.success();
    }

    @GetMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveCookie(request, JwtCookieFactory.REFRESH_TOKEN_COOKIE_NAME)
                .orElseThrow(() -> new CoreException(AuthErrorType.REFRESH_TOKEN_NOT_FOUND));

        AuthTokens tokens = authService.refresh(refreshToken);
        appendAuthCookies(tokens, request, response);
        return ApiResponse.success();
    }

    @DeleteMapping("/withdraw")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody WithdrawRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse response
    ) {
        authService.withdraw(authenticatedMember.memberId(), request.memberId());
        expireAuthCookies(httpServletRequest, response);
        return ApiResponse.success();
    }

    @PostMapping("/invest")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> invest(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody InvestRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse response
    ) {
        String accessToken = authService.updateInvest(
                authenticatedMember.memberId(),
                request.memberId(),
                request.investScore()
        );
        appendCookie(
                jwtCookieFactory.createAccessTokenCookie(accessToken, httpServletRequest),
                response
        );
        return ApiResponse.success();
    }

    private Optional<String> resolveCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void appendAuthCookies(
            AuthTokens tokens,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        appendCookie(jwtCookieFactory.createAccessTokenCookie(tokens.accessToken(), request), response);
        if (tokens.refreshToken() != null) {
            appendCookie(jwtCookieFactory.createRefreshTokenCookie(tokens.refreshToken(), request), response);
        }
    }

    private void expireAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        appendCookie(jwtCookieFactory.expireAccessTokenCookie(request), response);
        appendCookie(jwtCookieFactory.expireRefreshTokenCookie(request), response);
    }

    private void appendCookie(ResponseCookie cookie, HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
