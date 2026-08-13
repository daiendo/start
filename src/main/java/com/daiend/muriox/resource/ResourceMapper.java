package com.daiend.muriox.resource;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {

    @Select("""
            SELECT DISTINCT resource.code
            FROM sys_resource resource
            JOIN sys_role_resource role_resource
                ON role_resource.resource_id = resource.id
            JOIN sys_role role
                ON role.id = role_resource.role_id
            JOIN sys_user_role user_role
                ON user_role.role_id = role.id
            JOIN sys_user user_info
                ON user_info.id = user_role.user_id
            JOIN sys_menu menu
                ON menu.id = resource.menu_id
            JOIN sys_role_menu role_menu
                ON role_menu.role_id = role.id
               AND role_menu.menu_id = menu.id
            WHERE user_info.id = #{userId}
              AND user_info.enabled = TRUE
              AND role.status = TRUE
              AND menu.status = TRUE
              AND resource.status = TRUE
            ORDER BY resource.code
            """)
    List<String> findEnabledCodesByUserId(
            @Param("userId") Long userId);

    default boolean existsByCode(String code) {
        return selectCount(
                Wrappers.<Resource>lambdaQuery()
                        .eq(Resource::getCode, code)
        ) > 0;
    }


    default List<Resource> selectListByMenuId(Long menuId) {
        return  selectList(
                Wrappers.<Resource>lambdaQuery()
                        .eq(Resource::getMenuId, menuId)
                        .orderByAsc(Resource::getSortOrder)
                        .orderByAsc(Resource::getId));
    }
    default boolean existsByCodeExcludingId(
            String code,
            Long resourceId) {

        return selectCount(
                Wrappers.<Resource>lambdaQuery()
                        .eq(Resource::getCode, code)
                        .ne(Resource::getId, resourceId)
        ) > 0;
    }

    @Select("""
        SELECT DISTINCT user_role.user_id
        FROM sys_user_role user_role
        JOIN sys_role_resource role_resource
          ON role_resource.role_id = user_role.role_id
        WHERE role_resource.resource_id = #{resourceId}
        """)
    List<Long> findUserIdsByResourceId(
            @Param("resourceId") Long resourceId);

    List<Long> findUserIdsByResourceIds(
            @Param("resourceIds")
            Collection<Long> resourceIds);

    default boolean existsByMenuId(Long menuId) {
        return selectCount(
                Wrappers.<Resource>lambdaQuery()
                        .eq(Resource::getMenuId, menuId)
        ) > 0;
    }
    default boolean existsByMenuIds(
            Collection<Long> menuIds) {

        return selectCount(
                Wrappers.<Resource>lambdaQuery()
                        .in(Resource::getMenuId, menuIds)
        ) > 0;
    }

}
