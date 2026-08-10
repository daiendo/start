package com.daiend.muriox.user;

public record UserResponse (
        Long id,
        String account,
        String username,
        String avatar,
        String email,
        String mobile,
        String sex,
        String sexLabel,
        String remark
){
}
