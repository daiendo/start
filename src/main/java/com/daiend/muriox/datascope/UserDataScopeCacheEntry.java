package com.daiend.muriox.datascope;

import java.util.List;
import java.util.Objects;

public record UserDataScopeCacheEntry(
        long orgVersion,
        boolean all,
        List<Long> orgIds
) {
    public UserDataScopeCacheEntry {
        Objects.requireNonNull(
                orgIds,
                "组织 ID 列表不能为空");

        orgIds = List.copyOf(orgIds);
    }

    public static UserDataScopeCacheEntry from(
            long orgVersion,
            UserDataScope dataScope) {

        return new UserDataScopeCacheEntry(
                orgVersion,
                dataScope.all(),
                dataScope.orgIds());
    }

    public UserDataScope toDataScope() {
        return new UserDataScope(
                all,
                orgIds);
    }
}