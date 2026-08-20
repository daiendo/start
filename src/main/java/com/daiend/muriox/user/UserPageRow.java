package com.daiend.muriox.user;

import java.time.OffsetDateTime;

/**
 * 用户分页数据库查询结果。
 * <p>
 * avatar 保存对象 Key；
 * avatarUrl 不从数据库查询，由 Service 根据 avatar 生成。
 */
public record UserPageRow(
        Long id,
        Long orgId,
        String orgName,
        String account,
        String username,
        String avatar,
        String sex,
        Boolean enabled,
        String postNames,
        Boolean builtIn,
        String remark,
        OffsetDateTime createdAt
) {
}
