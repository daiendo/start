package com.daiend.muriox.menu;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
    default Page<Menu> selectRootPage(long current, long size) {
        Page<Menu> page = new Page<>(current, size);

        return selectPage(
                page,
                Wrappers.<Menu>lambdaQuery()
                        .isNull(Menu::getParentId)
                        .orderByAsc(Menu::getSortOrder)
                        .orderByAsc(Menu::getId)
        );
    }

    default List<Menu> findAllMenus() {
        return selectList(
                Wrappers.<Menu>lambdaQuery()
                        .orderByAsc(Menu::getParentId)
                        .orderByAsc(Menu::getSortOrder)
                        .orderByAsc(Menu::getId)
        );
    }

    @Select("""
            SELECT DISTINCT m.*
            FROM sys_menu m
            JOIN sys_role_menu rm
                ON rm.menu_id = m.id
            JOIN sys_role r
                ON r.id = rm.role_id
            JOIN sys_user_role ur
                ON ur.role_id = r.id
            JOIN sys_user u
                ON u.id = ur.user_id
            WHERE u.id = #{userId}
              AND u.enabled = TRUE
              AND r.status = TRUE
              AND m.status = TRUE
            ORDER BY
                m.parent_id NULLS FIRST,
                m.sort_order,
                m.id
            """)
    List<Menu> findEnabledMenusByUserId(
            @Param("userId") Long userId);

    default boolean existsByName(String name) {
        return selectCount(
                Wrappers.<Menu>lambdaQuery()
                        .eq(Menu::getName, name)
        ) > 0;
    }

    default boolean existsByPath(String path) {
        return selectCount(
                Wrappers.<Menu>lambdaQuery()
                        .eq(Menu::getPath, path)
        ) > 0;
    }

    default boolean existsByNameExcludingId(
            String name,
            Long menuId) {

        return selectCount(
                Wrappers.<Menu>lambdaQuery()
                        .eq(Menu::getName, name)
                        .ne(Menu::getId, menuId)
        ) > 0;
    }

    default boolean existsByPathExcludingId(
            String path,
            Long menuId) {

        return selectCount(
                Wrappers.<Menu>lambdaQuery()
                        .eq(Menu::getPath, path)
                        .ne(Menu::getId, menuId)
        ) > 0;
    }

    @Select("""
            SELECT DISTINCT user_role.user_id
            FROM sys_role_menu role_menu
            JOIN sys_user_role user_role
              ON user_role.role_id = role_menu.role_id
            WHERE role_menu.menu_id = #{menuId}
            """)
    List<Long> findUserIdsByMenuId(
            @Param("menuId") Long menuId);

    default boolean hasChildren(Long menuId) {
        return selectCount(
                Wrappers.<Menu>lambdaQuery()
                        .eq(Menu::getParentId, menuId)
        ) > 0;
    }

    List<Long> findUserIdsByMenuIds(
            @Param("menuIds")
            Collection<Long> menuIds);

    default boolean hasChildrenByParentIds(
            Collection<Long> menuIds) {

        return selectCount(
                Wrappers.<Menu>lambdaQuery()
                        .in(Menu::getParentId, menuIds)
        ) > 0;
    }

}
