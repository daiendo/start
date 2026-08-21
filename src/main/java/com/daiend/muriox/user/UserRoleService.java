package com.daiend.muriox.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daiend.muriox.auth.UserAuthorityChangePublisher;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.datascope.DataScopeGuard;
import com.daiend.muriox.role.Role;
import com.daiend.muriox.role.RoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserRoleService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserAuthorityChangePublisher
            authorityChangePublisher;
    private final DataScopeGuard dataScopeGuard;

    public UserRoleService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserAuthorityChangePublisher
                    authorityChangePublisher,
            DataScopeGuard dataScopeGuard) {

        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.authorityChangePublisher =
                authorityChangePublisher;
        this.dataScopeGuard = dataScopeGuard;
    }

    public UserRoleConfigResponse getConfig(
            Long userId) {

        validateUserId(userId);

        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(
                    "用户不存在");
        }

        List<UserRoleOptionResponse> roles =
                roleMapper.selectList(
                                Wrappers
                                        .<Role>lambdaQuery()
                                        .orderByAsc(
                                                Role::getSortOrder)
                                        .orderByAsc(
                                                Role::getId))
                        .stream()
                        .map(role ->
                                new UserRoleOptionResponse(
                                        role.getId(),
                                        role.getCode(),
                                        role.getName(),
                                        role.getStatus(),
                                        role.getBuiltIn()))
                        .toList();

        List<Long> selectedRoleIds =
                roleMapper.findRoleIdsByUserId(
                        userId);

        return new UserRoleConfigResponse(
                roles,
                selectedRoleIds);
    }

    @Transactional
    public void configureRoles(
            Long userId,
            UserRoleRequest request) {

        validateUserId(userId);

        User user =
                userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(
                    "用户不存在");
        }
        dataScopeGuard.assertOrgAllowed(
                user.getOrgId());

        if (Boolean.TRUE.equals(
                user.getBuiltIn())) {

            throw new BusinessException(
                    "系统内置用户的角色不能修改");
        }

        List<Long> roleIds =
                request.roleIds()
                        .stream()
                        .distinct()
                        .toList();

        List<Role> roles =
                roleIds.isEmpty()
                        ? List.of()
                        : roleMapper.selectByIds(
                        roleIds);

        if (roles.size() != roleIds.size()) {
            throw new BusinessException(
                    "部分角色不存在");
        }

        boolean hasEnabledRole =
                roles.stream()
                        .anyMatch(role ->
                                Boolean.TRUE.equals(
                                        role.getStatus()));

        if (Boolean.TRUE.equals(
                user.getEnabled())
                && !hasEnabledRole) {

            throw new BusinessException(
                    "启用中的用户必须至少保留一个启用角色");
        }

        roleMapper.deleteRolesByUserId(
                userId);

        if (!roleIds.isEmpty()) {
            int insertedRows =
                    roleMapper.insertUserRoles(
                            userId,
                            roleIds);

            if (insertedRows != roleIds.size()) {
                throw new BusinessException(
                        "配置用户角色失败");
            }
        }

        authorityChangePublisher.publishForUser(
                userId);
    }

    private void validateUserId(
            Long userId) {

        if (userId == null || userId <= 0) {
            throw new BusinessException(
                    "用户 ID 不合法");
        }
    }
}
