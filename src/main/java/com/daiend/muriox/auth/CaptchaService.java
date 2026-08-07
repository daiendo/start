package com.daiend.muriox.auth;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.wf.captcha.SpecCaptcha;

@Service
public class CaptchaService {

    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;
    private static final int LENGTH = 4;
    private static final long EXPIRES_IN_SECONDS = 120;
    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";

    private final StringRedisTemplate stringRedisTemplate;

    public CaptchaService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public CaptchaResponse create() {

        SpecCaptcha captcha = new SpecCaptcha(WIDTH, HEIGHT, LENGTH);
        String uuid = UUID.randomUUID().toString();
        String verCode = captcha.text();
        stringRedisTemplate.opsForValue().set(
                captchaKey(uuid),
                verCode,
                Duration.ofSeconds(EXPIRES_IN_SECONDS));

        return new CaptchaResponse(captcha.toBase64(), uuid, EXPIRES_IN_SECONDS);
    }

    public Boolean validate(String captchaUuid, String captchaCode) {
        if (captchaUuid == null || captchaCode == null) {
            return false;
        }
        String key = captchaKey(captchaUuid);
        String code = stringRedisTemplate
                .opsForValue()
                .getAndDelete(key);
        if (code == null) {
            return false;
        }
        return code.equalsIgnoreCase(captchaCode.trim());

    }

    private String captchaKey(String captchaId) {
        return CAPTCHA_KEY_PREFIX + captchaId;
    }
}
