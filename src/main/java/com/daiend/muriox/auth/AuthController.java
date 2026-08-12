package com.daiend.muriox.auth;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daiend.muriox.common.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/authority/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @SecurityRequirements
    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.ok(authService.createCaptcha());
    }

    @SecurityRequirements
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ApiResponse.ok(authService.login(loginRequest));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CurrentUser currentUser) {
        authService.logout(currentUser.sessionId());
        return ApiResponse.okMessage("注销成功");
    }

}
