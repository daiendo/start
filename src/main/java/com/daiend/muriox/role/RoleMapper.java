package com.daiend.muriox.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    default Page<Role> selectRolePage(
            long current,
            long size,
            String name) {

        Page<Role> page =
                new Page<>(current, size);

        LambdaQueryWrapper<Role> query =
                Wrappers.<Role>lambdaQuery();

        if (name != null && !name.isBlank()) {
            query.like(
                    Role::getName,
                    name.trim());
        }

        query.orderByAsc(Role::getSortOrder)
                .orderByAsc(Role::getId);

        return selectPage(page, query);
    }

    default boolean existsByName(String name) {
        return selectCount(
                Wrappers.<Role>lambdaQuery()
                        .eq(Role::getName, name)
        ) > 0;
    }

    default boolean existsByCode(String code) {
        return selectCount(
                Wrappers.<Role>lambdaQuery()
                        .eq(Role::getCode, code)
        ) > 0;
    }


    default boolean existsByCodeExcludingId(
            String code,
            Long roleId) {

        return selectCount(
                Wrappers.<Role>lambdaQuery()
                        .eq(Role::getCode, code)
                        .ne(Role::getId, roleId)
        ) > 0;
    }

    default boolean existsByNameExcludingId(
            String name,
            Long roleId) {

        return selectCount(
                Wrappers.<Role>lambdaQuery()
                        .eq(Role::getName, name)
                        .ne(Role::getId, roleId)
        ) > 0;
    }

    @Select("""
            SELECT DISTINCT user_role.user_id
            FROM sys_user_role user_role
            WHERE user_role.role_id = #{roleId}
            ORDER BY user_role.user_id
            """)
    List<Long> findUserIdsByRoleId(
            @Param("roleId") Long roleId);

    List<Long> findUserIdsByRoleIds(
            @Param("roleIds")
            Collection<Long> roleIds);

    @Select("""
            SELECT role_menu.menu_id
            FROM sys_role_menu role_menu
            WHERE role_menu.role_id = #{roleId}
            ORDER BY role_menu.menu_id
            """)
    List<Long> findMenuIdsByRoleId(
            @Param("roleId") Long roleId);

    @Select("""
            SELECT role_resource.resource_id
            FROM sys_role_resource role_resource
            WHERE role_resource.role_id = #{roleId}
            ORDER BY role_resource.resource_id
            """)
    List<Long> findResourceIdsByRoleId(
            @Param("roleId") Long roleId);


    int deleteMenusByRoleId(
            @Param("roleId") Long roleId);

    int deleteResourcesByRoleId(
            @Param("roleId") Long roleId);

    int insertRoleMenus(
            @Param("roleId") Long roleId,
            @Param("menuIds") Collection<Long> menuIds);

    int insertRoleResources(
            @Param("roleId") Long roleId,
            @Param("resourceIds") Collection<Long> resourceIds);

    int deleteUsersByRoleId(
            @Param("roleId") Long roleId);

    int insertRoleUsers(
            @Param("roleId") Long roleId,
            @Param("userIds") Collection<Long> userIds);

    List<Long> findRoleIdsByUserId(
            @Param("userId") Long userId);

    int deleteRolesByUserId(
            @Param("userId") Long userId);

    int insertUserRoles(
            @Param("userId") Long userId,
            @Param("roleIds") Collection<Long> roleIds);

    @Select("""
            SELECT role_data_org.org_id
            FROM sys_role_data_org role_data_org
            WHERE role_data_org.role_id = #{roleId}
            ORDER BY role_data_org.org_id
            """)
    List<Long> findDataOrgIdsByRoleId(
            @Param("roleId") Long roleId);

    int deleteDataOrgsByRoleId(
            @Param("roleId") Long roleId);

    int insertRoleDataOrgs(
            @Param("roleId") Long roleId,
            @Param("orgIds") Collection<Long> orgIds);
}
