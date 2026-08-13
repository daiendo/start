package com.daiend.muriox.role;

import java.util.List;

public record RoleAuthorityResponse(
        List<Long> menuIdList,
        List<Long> resourceIdList
) {

}
