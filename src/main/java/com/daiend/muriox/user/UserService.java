package com.daiend.muriox.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daiend.muriox.common.PageResult;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.organize.OrganizeMapper;
import com.daiend.muriox.post.Post;
import com.daiend.muriox.post.PostMapper;
import com.daiend.muriox.storage.ObjectStorageService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final OrganizeMapper organizeMapper;
    private final PostMapper postMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectStorageService objectStorageService;


    public UserService(UserMapper userMapper, OrganizeMapper organizeMapper, PostMapper postMapper, PasswordEncoder passwordEncoder, ObjectStorageService objectStorageService) {
        this.userMapper = userMapper;
        this.organizeMapper = organizeMapper;
        this.postMapper = postMapper;
        this.passwordEncoder = passwordEncoder;
        this.objectStorageService = objectStorageService;
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
}
