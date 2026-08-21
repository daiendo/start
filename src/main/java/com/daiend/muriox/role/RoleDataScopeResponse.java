package com.daiend.muriox.role;

import java.util.List;

public record RoleDataScopeResponse(
        DataScopeType dataScopeType,
        List<Long> orgIdList
) {
}