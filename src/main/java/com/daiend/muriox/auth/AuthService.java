package com.daiend.muriox.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.user.User;
import com.daiend.muriox.user.UserMapper;

@Service
public class AuthService {
    private final CaptchaService captchaService;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(CaptchaService captchaService, UserMapper userMapper, TokenService tokenService,
            PasswordEncoder passwordEncoder) {
        this.captchaService = captchaService;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public CaptchaResponse createCaptcha() {
        return captchaService.create();
    }

    public LoginResponse login(LoginRequest loginRequest) {
        if (!captchaService.validate(loginRequest.captchaUuid(), loginRequest.captchaCode())) {
            throw new BusinessException("验证码不正确");
        }

        User user = userMapper.findByAccount(loginRequest.account())
                .orElseThrow(() -> new BusinessException("账号或密码错误"));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }

       if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("账号已被禁用");
        }

        IssuedToken issuedToken = tokenService.createToken(user.getId());
        return new LoginResponse(issuedToken.token(),
                issuedToken.expiresInSeconds());

    }

    public void logout(String sessionId ) {
        tokenService.deleteToken(sessionId);
    }

}
