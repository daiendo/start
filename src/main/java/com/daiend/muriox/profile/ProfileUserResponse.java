package com.daiend.muriox.profile;

public record ProfileUserResponse (
        Long id,
        String account,
        String username,
        String email,
        String remark
){
}
