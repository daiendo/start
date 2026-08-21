package com.daiend.muriox.role;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daiend.muriox.auth.UserAuthorityChangePublisher;
import com.daiend.muriox.common.PageResult;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.menu.Menu;
import com.daiend.muriox.menu.MenuMapper;
import com.daiend.muriox.resource.Resource;
import com.daiend.muriox.resource.ResourceMapper;
import com.daiend.muriox.user.User;
import com.daiend.muriox.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RoleService {
    private final RoleMapper roleMapper;
    private final UserAuthorityChangePublisher authorityChangePublisher;
    private final MenuMapper menuMapper;
    private final ResourceMapper resourceMapper;
    private final UserMapper userMapper;

    public RoleService(RoleMapper roleMapper, UserAuthorityChangePublisher authorityChangePublisher, MenuMapper menuMapper, ResourceMapper resourceMapper, UserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.authorityChangePublisher = authorityChangePublisher;
        this.menuMapper = menuMapper;
        this.resourceMapper = resourceMapper;
        this.userMapper = userMapper;
    }


    public PageResult<RoleResponse> page(
            long current,
            long size,
            String name) {

        validatePageParams(current, size);

        Page<Role> rolePage =
                roleMapper.selectRolePage(
                        current,
                        size,
                        name);

        List<RoleResponse> records =
                rolePage.getRecords()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return new PageResult<>(
                records,
                rolePage.getTotal(),
                rolePage.getCurrent(),
                rolePage.getSize(),
                rolePage.getPages());
    }

    @Transactional
    public Long create(RoleRequest request) {
        String name = request.name().trim();

        String code = request.code()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (roleMapper.existsByName(name)) {
            throw new BusinessException(
                    "角色名称已存在");
        }

        if (roleMapper.existsByCode(code)) {
            throw new BusinessException(
                    "角色编码已存在");
        }

        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setStatus(request.status());
        role.setSortOrder(request.sortOrder());
        role.setRemark(
                normalizeOptional(request.remark()));

        int affectedRows =
                roleMapper.insert(role);

        if (affectedRows != 1) {
            throw new BusinessException(
                    "新增角色失败");
        }

        return role.getId();
    }


    public RoleResponse getRole(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("角色id不合法");
        }

        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return toResponse(role);
    }

    @Transactional
    public Long update(RoleUpdateRequest updateRequest) {

        Role role = roleMapper.selectById(updateRequest.id());
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        String code = updateRequest.code()
                .trim()
                .toLowerCase(Locale.ROOT);

        String name = updateRequest.name().trim();


        validateBuiltInRoleUpdate(
                role,
                code,
                updateRequest.status());

        if (roleMapper.existsByCodeExcludingId(
                code,
                updateRequest.id())) {

            throw new BusinessException(
                    "角色编码已存在");
        }
        if (roleMapper.existsByNameExcludingId(name, updateRequest.id())) {
            throw new BusinessException("角色名称已存在");
        }
        List<Long> affectedUserIds =
                roleMapper.findUserIdsByRoleId(
                        role.getId());
        role.setCode(code);
        role.setName(name);
        role.setStatus(updateRequest.status());
        role.setSortOrder(updateRequest.sortOrder());
        role.setRemark(normalizeOptional(updateRequest.remark()));
        int affectedRows =
                roleMapper.updateById(role);
        if (affectedRows != 1) {
            throw new BusinessException("编辑角色失败");
        }
        authorityChangePublisher.publishForUsers(
                affectedUserIds);
        return role.getId();
    }

    @Transactional
    public void deleteRoles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(
                    "请选择要删除的角色");
        }

        if (ids.stream().anyMatch(
                id -> id == null || id <= 0)) {

            throw new BusinessException(
                    "角色 ID 不合法");
        }

        List<Long> roleIds =
                ids.stream()
                        .distinct()
                        .toList();

        List<Role> roles =
                roleMapper.selectByIds(roleIds);

        if (roles.size() != roleIds.size()) {
            throw new BusinessException(
                    "部分角色不存在");
        }
        boolean containsBuiltInRole =
                roles.stream()
                        .anyMatch(role ->
                                Boolean.TRUE.equals(
                                        role.getBuiltIn()));

        if (containsBuiltInRole) {
            throw new BusinessException(
                    "系统内置角色不能删除");
        }

        /*
         * 必须在删除角色和 sys_user_role
         * 关联关系之前查询。
         */
        List<Long> affectedUserIds =
                roleMapper.findUserIdsByRoleIds(
                        roleIds);

        int affectedRows =
                roleMapper.deleteByIds(roleIds);

        if (affectedRows != roleIds.size()) {
            throw new BusinessException(
                    "角色删除失败");
        }

        authorityChangePublisher.publishForUsers(
                affectedUserIds);
    }


    public RoleAuthorityResponse getAuthority(
            Long roleId) {

        if (roleId == null || roleId <= 0) {
            throw new BusinessException(
                    "角色 ID 不合法");
        }

        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(
                    "角色不存在");
        }

        List<Long> menuIds =
                roleMapper.findMenuIdsByRoleId(
                        roleId);

        List<Long> resourceIds =
                roleMapper.findResourceIdsByRoleId(
                        roleId);

        return new RoleAuthorityResponse(
                menuIds,
                resourceIds);
    }

    public List<Long> getUsers(Long roleId) {
        if (roleId == null || roleId <= 0) {
            throw new BusinessException(
                    "角色 ID 不合法");
        }

        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(
                    "角色不存在");
        }

        return roleMapper.findUserIdsByRoleId(
                roleId);
    }

    @Transactional
    public void configureAuthority(
            RoleAuthorityRequest request) {

        Role role =
                roleMapper.selectById(
                        request.roleId());

        if (role == null) {
            throw new BusinessException(
                    "角色不存在");
        }

        List<Long> menuIds =
                request.menuIdList()
                        .stream()
                        .distinct()
                        .toList();

        List<Long> resourceIds =
                request.resourceIdList()
                        .stream()
                        .distinct()
                        .toList();

        List<Menu> menus =
                menuIds.isEmpty()
                        ? List.of()
                        : menuMapper.selectByIds(menuIds);

        if (menus.size() != menuIds.size()) {
            throw new BusinessException(
                    "部分菜单不存在");
        }

        List<Resource> resources =
                resourceIds.isEmpty()
                        ? List.of()
                        : resourceMapper.selectByIds(
                        resourceIds);

        if (resources.size() != resourceIds.size()) {
            throw new BusinessException(
                    "部分按钮权限不存在");
        }

        Set<Long> selectedMenuIds =
                new HashSet<>(menuIds);

        boolean containsUnselectedMenuResource =
                resources.stream()
                        .anyMatch(resource ->
                                !selectedMenuIds.contains(
                                        resource.getMenuId()));

        if (containsUnselectedMenuResource) {
            throw new BusinessException(
                    "按钮权限所属菜单未被选中");
        }

        /*
         * 必须在替换关联关系前查询受影响用户。
         */
        List<Long> affectedUserIds =
                roleMapper.findUserIdsByRoleId(
                        role.getId());

        roleMapper.deleteResourcesByRoleId(
                role.getId());

        roleMapper.deleteMenusByRoleId(
                role.getId());

        if (!menuIds.isEmpty()) {
            int insertedMenus =
                    roleMapper.insertRoleMenus(
                            role.getId(),
                            menuIds);

            if (insertedMenus != menuIds.size()) {
                throw new BusinessException(
                        "配置角色菜单权限失败");
            }
        }

        if (!resourceIds.isEmpty()) {
            int insertedResources =
                    roleMapper.insertRoleResources(
                            role.getId(),
                            resourceIds);

            if (insertedResources
                    != resourceIds.size()) {

                throw new BusinessException(
                        "配置角色按钮权限失败");
            }
        }

        authorityChangePublisher.publishForUsers(
                affectedUserIds);
    }


    @Transactional
    public void configureUsers(
            RoleUserRequest request) {

        Role role =
                roleMapper.selectById(
                        request.roleId());

        if (role == null) {
            throw new BusinessException(
                    "角色不存在");
        }

        List<Long> userIds =
                request.userIdList()
                        .stream()
                        .distinct()
                        .toList();

        List<User> users =
                userIds.isEmpty()
                        ? List.of()
                        : userMapper.selectByIds(userIds);

        if (users.size() != userIds.size()) {
            throw new BusinessException(
                    "部分用户不存在");
        }


        /*
         * 被移除角色的旧用户和新授权用户都需要
         * 清理权限缓存。
         */
        List<Long> oldUserIds =
                roleMapper.findUserIdsByRoleId(
                        role.getId());

        Set<Long> oldUserIdSet =
                new HashSet<>(oldUserIds);

        Set<Long> requestedUserIdSet =
                new HashSet<>(userIds);

        /*
         * 只检查角色成员关系真正发生变化的用户。
         * 仍然保留在该角色中的内置用户不受影响。
         */
        List<Long> membershipChangedUserIds =
                java.util.stream.Stream.concat(
                                oldUserIds.stream(),
                                userIds.stream())
                        .filter(userId ->
                                oldUserIdSet.contains(userId)
                                        != requestedUserIdSet.contains(
                                        userId))
                        .distinct()
                        .toList();

        List<User> membershipChangedUsers =
                membershipChangedUserIds.isEmpty()
                        ? List.of()
                        : userMapper.selectByIds(
                        membershipChangedUserIds);

        boolean containsBuiltInUser =
                membershipChangedUsers.stream()
                        .anyMatch(user ->
                                Boolean.TRUE.equals(
                                        user.getBuiltIn()));

        if (containsBuiltInUser) {
            throw new BusinessException(
                    "系统内置用户的角色归属不能修改");
        }

        List<Long> affectedUserIds =
                java.util.stream.Stream.concat(
                                oldUserIds.stream(),
                                userIds.stream())
                        .distinct()
                        .toList();

        roleMapper.deleteUsersByRoleId(
                role.getId());

        if (!userIds.isEmpty()) {
            int insertedRows =
                    roleMapper.insertRoleUsers(
                            role.getId(),
                            userIds);

            if (insertedRows != userIds.size()) {
                throw new BusinessException(
                        "角色用户授权失败");
            }
        }

        if (!affectedUserIds.isEmpty()) {
            List<Long> invalidUserIds =
                    userMapper
                            .findEnabledUserIdsWithoutEnabledRole(
                                    affectedUserIds);

            if (!invalidUserIds.isEmpty()) {
                throw new BusinessException(
                        "启用中的用户必须至少保留一个启用角色");
            }
        }

        authorityChangePublisher.publishForUsers(
                affectedUserIds);
    }

    private void validatePageParams(
            long current,
            long size) {

        if (current < 1) {
            throw new BusinessException(
                    "当前页必须大于等于 1");
        }

        if (size < 1) {
            throw new BusinessException(
                    "每页数量必须大于等于 1");
        }

        if (size > 100) {
            throw new BusinessException(
                    "每页数量不能超过 100");
        }
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getBuiltIn(),
                role.getStatus(),
                role.getSortOrder(),
                role.getRemark(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validateBuiltInRoleUpdate(
            Role role,
            String requestedCode,
            Boolean requestedStatus) {

        if (!Boolean.TRUE.equals(role.getBuiltIn())) {
            return;
        }

        if (!java.util.Objects.equals(
                role.getCode(),
                requestedCode)) {

            throw new BusinessException(
                    "系统内置角色的编码不能修改");
        }

        if (!java.util.Objects.equals(
                role.getStatus(),
                requestedStatus)) {

            throw new BusinessException(
                    "系统内置角色的状态不能修改");
        }
    }

}
