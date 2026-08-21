package com.daiend.muriox.datascope;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record UserDataScope(
        boolean all,
        List<Long> orgIds
) {
    public UserDataScope {
        Objects.requireNonNull(
                orgIds,
                "组织 ID 列表不能为空");

        orgIds = all
                ? List.of()
                : orgIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    public static UserDataScope allData() {
        return new UserDataScope(
                true,
                List.of());
    }

    public static UserDataScope denied() {
        return new UserDataScope(
                false,
                List.of());
    }

    public static UserDataScope organizations(
            Collection<Long> orgIds) {

        return new UserDataScope(
                false,
                List.copyOf(orgIds));
    }

    public boolean isDenied() {
        return !all && orgIds.isEmpty();
    }

    public boolean allows(Long orgId) {
        return all
                || orgId != null
                && orgIds.contains(orgId);
    }
}