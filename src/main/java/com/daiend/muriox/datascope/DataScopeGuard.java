package com.daiend.muriox.datascope;

import com.daiend.muriox.auth.CurrentUser;
import com.daiend.muriox.role.DataScopeType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class DataScopeGuard {

    private final UserDataScopeService dataScopeService;

    public DataScopeGuard(
            UserDataScopeService dataScopeService) {

        this.dataScopeService = dataScopeService;
    }

    public UserDataScope currentScope() {
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof CurrentUser currentUser)) {

            throw denied();
        }

        return dataScopeService.getDataScope(
                currentUser.id());
    }

    public void assertOrgAllowed(Long orgId) {
        if (!currentScope().allows(orgId)) {
            throw denied();
        }
    }

    public void assertAllOrgsAllowed(
            Collection<Long> orgIds) {

        UserDataScope scope = currentScope();
        boolean containsDeniedOrg = orgIds == null
                || orgIds.stream()
                .anyMatch(orgId -> !scope.allows(orgId));

        if (containsDeniedOrg) {
            throw denied();
        }
    }

    public void assertAllData() {
        if (!currentScope().all()) {
            throw denied();
        }
    }

    public void assertCanConfigureRoleDataScope(
            DataScopeType dataScopeType,
            Collection<Long> customOrgIds) {

        UserDataScope scope = currentScope();
        if (scope.all()) {
            return;
        }

        if (dataScopeType != DataScopeType.CUSTOM_ORG) {
            throw denied();
        }

        boolean containsDeniedOrg = customOrgIds == null
                || customOrgIds.isEmpty()
                || customOrgIds.stream()
                .anyMatch(orgId -> !scope.allows(orgId));

        if (containsDeniedOrg) {
            throw denied();
        }
    }

    private DataScopeAccessDeniedException denied() {
        return new DataScopeAccessDeniedException(
                "无权操作该组织的数据");
    }
}
