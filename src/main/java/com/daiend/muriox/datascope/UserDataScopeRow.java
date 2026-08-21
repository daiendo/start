package com.daiend.muriox.datascope;

import com.daiend.muriox.role.DataScopeType;

public record UserDataScopeRow(
        Long userId,
        Long userOrgId,
        Long roleId,
        DataScopeType dataScopeType,
        Long customOrgId
) {
}