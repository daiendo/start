package com.daiend.muriox.auth;

public record CaptchaResponse(String image, String uuid, long expires) {

}
