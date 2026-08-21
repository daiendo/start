package com.daiend.muriox.user;

import java.util.List;

public record UserRoleConfigResponse(
        List<UserRoleOptionResponse> roles,
        List<Long> roleIds
) {
}