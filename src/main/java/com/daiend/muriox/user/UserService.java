package com.daiend.muriox.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daiend.muriox.auth.UserAuthorityChangePublisher;
import com.daiend.muriox.auth.UserSessionRevocationPublisher;
import com.daiend.muriox.common.PageResult;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.organize.OrganizeMapper;
import com.daiend.muriox.post.Post;
import com.daiend.muriox.post.PostMapper;
import com.daiend.muriox.storage.ObjectStorageService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final OrganizeMapper organizeMapper;
    private final PostMapper postMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectStorageService objectStorageService;
    private final UserAuthorityChangePublisher authorityChangePublisher;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    private final UserSessionRevocationPublisher sessionRevocationPublisher;

    public UserService(
            UserMapper userMapper,
            OrganizeMapper organizeMapper,
            PostMapper postMapper,
            PasswordEncoder passwordEncoder,
            ObjectStorageService objectStorageService,
            UserAuthorityChangePublisher authorityChangePublisher,
            TemporaryPasswordGenerator temporaryPasswordGenerator,
            UserSessionRevocationPublisher sessionRevocationPublisher) {
        this.userMapper = userMapper;
        this.organizeMapper = organizeMapper;
        this.postMapper = postMapper;
        this.passwordEncoder = passwordEncoder;
        this.objectStorageService = objectStorageService;
        this.authorityChangePublisher = authorityChangePublisher;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.sessionRevocationPublisher = sessionRevocationPublisher;
    }

    public List<UserListResponse> getUsers(
            String username) {

        return userMapper.getUsers(username)
                .stream()
                .map(this::toUserListResponse)
                .toList();
    }

    @Transactional
    public Long add(UserRequest request) {
        String account = request.account().trim();
        String username = request.username().trim();

        String email = normalizeOptional(request.email());
        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
        }

        String mobile = normalizeOptional(request.mobile());
        Long orgId = request.orgId();

        List<Long> postIds = request.postIds()
                .stream()
                .distinct()
                .toList();

        if (userMapper.existsByAccount(account)) {
            throw new BusinessException("登录账号已存在");
        }

        if (email != null && userMapper.existsByEmail(email)) {
            throw new BusinessException("邮箱已存在");
        }

        if (mobile != null && userMapper.existsByMobile(mobile)) {
            throw new BusinessException("手机号已存在");
        }

        if (organizeMapper.selectById(orgId) == null) {
            throw new BusinessException("所属组织不存在");
        }

        List<Post> posts = postIds.isEmpty()
                ? List.of()
                : postMapper.selectByIds(postIds);

        if (posts.size() != postIds.size()) {
            throw new BusinessException("部分岗位不存在");
        }

        boolean containsOtherOrganizationPost =
                posts.stream()
                        .anyMatch(post ->
                                !Objects.equals(
                                        post.getOrgId(),
                                        orgId));

        if (containsOtherOrganizationPost) {
            throw new BusinessException(
                    "只能选择所属组织的直属岗位");
        }

        User user = new User();
        user.setAccount(account);
        user.setUsername(username);
        user.setOrgId(orgId);
        user.setAvatar(
                normalizeOptional(request.avatar()));
        user.setSex(
                request.sex() == null
                        ? "unknown"
                        : request.sex());
        user.setEmail(email);
        user.setMobile(mobile);
        user.setBuiltIn(false);

        /*
         * 新用户没有初始可用密码。
         * 后续需要通过重置密码流程设置密码。
         */
        user.setPasswordHash(
                passwordEncoder.encode(
                        UUID.randomUUID().toString()));

        user.setEnabled(false);
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(null);
        user.setRemark(
                normalizeOptional(request.remark()));

        int affectedRows = userMapper.insert(user);

        if (affectedRows != 1) {
            throw new BusinessException("新增用户失败");
        }

        if (!postIds.isEmpty()) {
            int insertedRows =
                    userMapper.insertUserPosts(
                            user.getId(),
                            postIds);

            if (insertedRows != postIds.size()) {
                throw new BusinessException(
                        "保存用户岗位失败");
            }
        }

        return user.getId();
    }


    public UserDetailResponse getDetail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    "用户 ID 不合法");
        }

        UserDetailRow user =
                userMapper.selectUserDetail(id);

        if (user == null) {
            throw new BusinessException(
                    "用户不存在");
        }

        List<UserPostRow> posts =
                userMapper.selectUserPosts(id);

        List<Long> postIds =
                posts.stream()
                        .map(UserPostRow::postId)
                        .toList();

        String postNames =
                posts.stream()
                        .map(UserPostRow::postName)
                        .collect(Collectors.joining("、"));

        return new UserDetailResponse(
                user.id(),
                user.account(),
                user.username(),
                user.orgId(),
                user.orgName(),
                postIds,
                postNames,
                user.avatar(),
                createAvatarUrl(user.avatar()),
                user.sex(),
                user.email(),
                user.mobile(),
                user.enabled(),
                user.builtIn(),
                user.remark(),
                user.createdAt(),
                user.updatedAt());
    }


    @Transactional
    public Long update(UserUpdateRequest request) {
        User user = userMapper.selectById(request.id());

        if (user == null) {
            throw new BusinessException(
                    "用户不存在");
        }

        String account = request.account().trim();
        String username = request.username().trim();

        String email = normalizeOptional(request.email());
        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
        }

        String mobile = normalizeOptional(request.mobile());
        Long orgId = request.orgId();

        List<Long> postIds =
                request.postIds()
                        .stream()
                        .distinct()
                        .toList();

        if (Boolean.TRUE.equals(user.getEnabled())
                && postIds.isEmpty()) {

            throw new BusinessException(
                    "启用中的用户必须至少保留一个岗位");
        }

        if (userMapper.existsByAccountExcludingId(
                account,
                user.getId())) {

            throw new BusinessException(
                    "登录账号已存在");
        }

        if (email != null
                && userMapper.existsByEmailExcludingId(
                email,
                user.getId())) {

            throw new BusinessException(
                    "邮箱已存在");
        }

        if (mobile != null
                && userMapper.existsByMobileExcludingId(
                mobile,
                user.getId())) {

            throw new BusinessException(
                    "手机号已存在");
        }

        if (organizeMapper.selectById(orgId) == null) {
            throw new BusinessException(
                    "所属组织不存在");
        }

        List<Post> posts =
                postIds.isEmpty()
                        ? List.of()
                        : postMapper.selectByIds(postIds);

        if (posts.size() != postIds.size()) {
            throw new BusinessException(
                    "部分岗位不存在");
        }

        boolean containsOtherOrganizationPost =
                posts.stream()
                        .anyMatch(post ->
                                !Objects.equals(
                                        post.getOrgId(),
                                        orgId));

        if (containsOtherOrganizationPost) {
            throw new BusinessException(
                    "只能选择所属组织的直属岗位");
        }

        user.setAccount(account);
        user.setUsername(username);
        user.setOrgId(orgId);
        user.setAvatar(
                normalizeOptional(request.avatar()));
        user.setSex(
                request.sex() == null
                        ? "unknown"
                        : request.sex());
        user.setEmail(email);
        user.setMobile(mobile);
        user.setRemark(
                normalizeOptional(request.remark()));

        if (userMapper.updateById(user) != 1) {
            throw new BusinessException(
                    "编辑用户失败");
        }

        userMapper.deleteUserPosts(user.getId());

        if (!postIds.isEmpty()) {
            int insertedRows =
                    userMapper.insertUserPosts(
                            user.getId(),
                            postIds);

            if (insertedRows != postIds.size()) {
                throw new BusinessException(
                        "保存用户岗位失败");
            }
        }

        authorityChangePublisher.publishForUser(
                user.getId());

        return user.getId();
    }


    @Transactional
    public UserPasswordResetResponse resetPassword(
            Long targetUserId,
            Long operatorUserId) {

        if (targetUserId == null
                || targetUserId <= 0) {

            throw new BusinessException(
                    "用户 ID 不合法");
        }

        if (Objects.equals(
                targetUserId,
                operatorUserId)) {

            throw new BusinessException(
                    "不能通过管理员重置功能修改自己的密码");
        }

        User user =
                userMapper.selectById(
                        targetUserId);

        if (user == null) {
            throw new BusinessException(
                    "用户不存在");
        }

        if (Boolean.TRUE.equals(
                user.getBuiltIn())) {

            throw new BusinessException(
                    "内置系统管理员密码不能通过此处重置");
        }

        String temporaryPassword =
                temporaryPasswordGenerator.generate();

        User passwordUpdate = new User();
        passwordUpdate.setId(user.getId());
        passwordUpdate.setPasswordHash(
                passwordEncoder.encode(
                        temporaryPassword));
        passwordUpdate.setMustChangePassword(true);
        passwordUpdate.setPasswordChangedAt(
                LocalDateTime.now());

        if (userMapper.updateById(
                passwordUpdate) != 1) {

            throw new BusinessException(
                    "重置用户密码失败");
        }

        sessionRevocationPublisher.publishForUser(
                user.getId());

        return new UserPasswordResetResponse(
                temporaryPassword);
    }

    public PageResult<UserPageResponse> page(
            long current,
            long size,
            String account,
            Long orgId) {

        validatePageParams(current, size);

        if (orgId != null && orgId <= 0) {
            throw new BusinessException(
                    "组织 ID 不合法");
        }

        String normalizedAccount =
                normalizeOptional(account);

        Page<UserPageRow> pageRequest =
                new Page<>(current, size);

        Page<UserPageRow> userPage =
                userMapper.selectUserPage(
                        pageRequest,
                        normalizedAccount,
                        orgId);

        List<UserPageResponse> records =
                userPage.getRecords()
                        .stream()
                        .map(this::toPageResponse)
                        .toList();

        return new PageResult<>(
                records,
                userPage.getTotal(),
                userPage.getCurrent(),
                userPage.getSize(),
                userPage.getPages());
    }

    @Transactional
    public void changeEnabled(
            Long targetUserId,
            Boolean enabled,
            Long operatorUserId) {

        if (targetUserId == null
                || targetUserId <= 0) {

            throw new BusinessException(
                    "用户 ID 不合法");
        }

        if (Objects.equals(
                targetUserId,
                operatorUserId)) {

            throw new BusinessException(
                    "不能修改自己的启用状态");
        }

        User user =
                userMapper.selectById(
                        targetUserId);

        if (user == null) {
            throw new BusinessException(
                    "用户不存在");
        }

        if (Boolean.TRUE.equals(
                user.getBuiltIn())
                && !Boolean.TRUE.equals(enabled)) {

            throw new BusinessException(
                    "内置系统管理员不能禁用");
        }

        if (Objects.equals(
                user.getEnabled(),
                enabled)) {

            return;
        }

        if (Boolean.TRUE.equals(enabled)) {
            validateUserCanBeEnabled(user);
        }

        User statusUpdate = new User();
        statusUpdate.setId(user.getId());
        statusUpdate.setEnabled(enabled);

        if (userMapper.updateById(
                statusUpdate) != 1) {

            throw new BusinessException(
                    enabled
                            ? "启用用户失败"
                            : "禁用用户失败");
        }

        /*
         * 启用和禁用都会改变菜单、按钮权限的
         * 实际查询结果。
         */
        authorityChangePublisher.publishForUser(
                user.getId());

        if (!Boolean.TRUE.equals(enabled)) {
            /*
             * 禁用后必须撤销所有登录设备。
             */
            sessionRevocationPublisher.publishForUser(
                    user.getId());
        }
    }

    @Transactional
    public void deleteUsers(
            List<Long> ids,
            Long operatorUserId) {

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(
                    "请选择需要删除的用户");
        }

        if (ids.stream().anyMatch(
                id -> id == null || id <= 0)) {

            throw new BusinessException(
                    "用户 ID 不合法");
        }

        List<Long> userIds =
                ids.stream()
                        .distinct()
                        .toList();

        if (userIds.contains(operatorUserId)) {
            throw new BusinessException(
                    "不能删除当前登录用户");
        }

        List<User> users =
                userMapper.selectByIds(userIds);

        if (users.size() != userIds.size()) {
            throw new BusinessException(
                    "部分用户不存在");
        }

        boolean containsBuiltInUser =
                users.stream()
                        .anyMatch(user ->
                                Boolean.TRUE.equals(
                                        user.getBuiltIn()));

        if (containsBuiltInUser) {
            throw new BusinessException(
                    "系统内置用户不能删除");
        }

        userMapper.deleteUserPostsByUserIds(
                userIds);

        userMapper.deleteUserRolesByUserIds(
                userIds);

        int affectedRows =
                userMapper.deleteByIds(userIds);

        if (affectedRows != userIds.size()) {
            throw new BusinessException(
                    "删除用户失败");
        }

        /*
         * 两类事件均在事务提交后执行：
         * 1. 删除菜单、按钮权限缓存；
         * 2. 撤销该用户的全部登录会话。
         */
        authorityChangePublisher.publishForUsers(
                userIds);

        sessionRevocationPublisher.publishForUsers(
                userIds);
    }

    private UserListResponse toUserListResponse(User user) {
        return new UserListResponse(
                user.getId(),
                user.getAccount(),
                user.getUsername(),
                user.getEnabled());
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }


    private UserPageResponse toPageResponse(
            UserPageRow row) {

        String avatarUrl = null;

        if (row.avatar() != null
                && !row.avatar().isBlank()) {

            avatarUrl =
                    objectStorageService
                            .createPresignedGetUrl(
                                    row.avatar());
        }

        return new UserPageResponse(
                row.id(),
                row.orgId(),
                row.orgName(),
                row.account(),
                row.username(),
                row.avatar(),
                avatarUrl,
                row.sex(),
                row.enabled(),
                row.postNames(),
                row.builtIn(),
                row.remark(),
                row.createdAt());
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

    private String createAvatarUrl(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return null;
        }

        return objectStorageService
                .createPresignedGetUrl(avatar);
    }


    private void validateUserCanBeEnabled(
            User user) {

        if (user.getPasswordChangedAt() == null) {
            throw new BusinessException(
                    "请先重置用户密码");
        }

        if (organizeMapper.selectById(
                user.getOrgId()) == null) {

            throw new BusinessException(
                    "用户所属组织不存在");
        }

        List<Long> postIds =
                userMapper.findPostIdsByUserId(
                        user.getId());

        if (postIds.isEmpty()) {
            throw new BusinessException(
                    "启用用户必须至少分配一个岗位");
        }

        List<Post> posts =
                postMapper.selectByIds(
                        postIds);

        if (posts.size() != postIds.size()) {
            throw new BusinessException(
                    "用户的部分岗位不存在");
        }

        boolean containsOtherOrganizationPost =
                posts.stream()
                        .anyMatch(post ->
                                !Objects.equals(
                                        post.getOrgId(),
                                        user.getOrgId()));

        if (containsOtherOrganizationPost) {
            throw new BusinessException(
                    "用户岗位必须直属所属组织");
        }

        boolean containsDisabledPost =
                posts.stream()
                        .anyMatch(post ->
                                !Boolean.TRUE.equals(
                                        post.getStatus()));

        if (containsDisabledPost) {
            throw new BusinessException(
                    "用户存在已禁用岗位");
        }

        if (!userMapper.existsEnabledRoleByUserId(
                user.getId())) {

            throw new BusinessException(
                    "启用用户必须至少分配一个启用角色");
        }
    }

}
