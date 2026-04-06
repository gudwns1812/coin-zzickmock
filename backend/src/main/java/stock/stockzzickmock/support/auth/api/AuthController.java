package stock.stockzzickmock.support.auth.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import stock.stockzzickmock.support.error.AuthErrorType;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.auth.api.dto.request.DuplicateAccountRequest;
import stock.stockzzickmock.support.auth.api.dto.request.InvestRequest;
import stock.stockzzickmock.support.auth.api.dto.request.LoginRequest;
import stock.stockzzickmock.support.auth.api.dto.request.RegisterRequest;
import stock.stockzzickmock.support.auth.api.dto.request.WithdrawRequest;
import stock.stockzzickmock.support.auth.security.AuthenticatedMember;
import stock.stockzzickmock.support.auth.security.JwtCookieFactory;
import stock.stockzzickmock.support.auth.application.AuthService;
import stock.stockzzickmock.support.response.ApiResponse;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success();
    }

    @PostMapping("/duplicate")
    public ResponseEntity<ApiResponse<Boolean>> duplicate(@Valid @RequestBody DuplicateAccountRequest request) {
        if (!authService.isAccountAvailable(request.account())) {
            throw new CoreException(AuthErrorType.DUPLICATE_ACCOUNT);
        }
        return ResponseEntity.ok(ApiResponse.success(true));
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        appendCookies(authService.login(request, httpServletRequest), httpServletResponse);
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        appendCookies(authService.logout(authenticatedMember, request), response);
        return ApiResponse.success();
    }

    @GetMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveCookie(request, JwtCookieFactory.REFRESH_TOKEN_COOKIE_NAME)
                .orElseThrow(() -> new CoreException(AuthErrorType.REFRESH_TOKEN_NOT_FOUND));

        appendCookies(authService.refresh(refreshToken, request), response);
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
        appendCookies(authService.withdraw(authenticatedMember, request, httpServletRequest), response);
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
        appendCookies(authService.updateInvest(authenticatedMember, request, httpServletRequest), response);
        return ApiResponse.success();
    }

    private java.util.Optional<String> resolveCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void appendCookies(List<String> cookieHeaders, HttpServletResponse response) {
        for (String cookieHeader : cookieHeaders) {
            response.addHeader("Set-Cookie", cookieHeader);
        }
    }
}
