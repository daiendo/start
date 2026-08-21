package com.daiend.muriox.user;

import com.daiend.muriox.auth.CurrentUser;
import com.daiend.muriox.common.ApiResponse;
import com.daiend.muriox.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/authority/user")
public class UserController {

    private final UserService userService;
    private final AvatarService avatarService;
    private final UserRoleService userRoleService;

    public UserController(UserService userService, AvatarService avatarService, UserRoleService userRoleService) {
        this.userService = userService;
        this.avatarService = avatarService;
        this.userRoleService = userRoleService;
    }

    @GetMapping("/list")
    public ApiResponse<List<UserListResponse>> getUsers(@RequestParam(required = false) String username) {

        return ApiResponse.ok(userService.getUsers(username));

    }

    @PostMapping(
            value = "/avatar",
            consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('user:upload:avatar')")
    public ApiResponse<AvatarResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file) {

        return ApiResponse.ok(
                avatarService.upload(file));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:add')")
    public ApiResponse<Long> add(
            @Valid @RequestBody UserRequest userRequest) {

        return ApiResponse.ok(userService.add(userRequest));
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<UserPageResponse>> page(
            @RequestParam(defaultValue = "1")
            long current,

            @RequestParam(defaultValue = "10")
            long size,

            @RequestParam(required = false)
            String account,

            @RequestParam(required = false)
            Long orgId) {

        return ApiResponse.ok(
                userService.page(
                        current,
                        size,
                        account,
                        orgId));
    }

    @GetMapping("/{id}/roles")
    public ApiResponse<UserRoleConfigResponse>
    getRoleConfig(
            @PathVariable Long id) {

        return ApiResponse.ok(
                userRoleService.getConfig(id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize(
            "hasAuthority('user:role:authorization')")
    public ApiResponse<Void> configureRoles(
            @PathVariable Long id,

            @Valid @RequestBody
            UserRoleRequest request) {

        userRoleService.configureRoles(
                id,
                request);

        return ApiResponse.okMessage(
                "配置用户角色成功");
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDetailResponse> getDetail(
            @PathVariable Long id) {

        return ApiResponse.ok(
                userService.getDetail(id));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('user:edit')")
    public ApiResponse<Long> update(
            @Valid @RequestBody
            UserUpdateRequest request) {

        return ApiResponse.ok(
                userService.update(request));
    }


    @PostMapping("/{id}/password/reset")
    @PreAuthorize(
            "hasAuthority('user:reset:password')")
    public ApiResponse<UserPasswordResetResponse>
    resetPassword(
            @PathVariable Long id,

            @AuthenticationPrincipal
            CurrentUser currentUser) {

        return ApiResponse.ok(
                userService.resetPassword(
                        id,
                        currentUser.id()));
    }

    @PutMapping("/{id}/enabled")
    @PreAuthorize(
            "hasAuthority('user:change:status')")
    public ApiResponse<Void> changeEnabled(
            @PathVariable Long id,

            @Valid @RequestBody
            UserEnabledRequest request,

            @AuthenticationPrincipal
            CurrentUser currentUser) {

        userService.changeEnabled(
                id,
                request.enabled(),
                currentUser.id());

        return ApiResponse.okMessage(
                Boolean.TRUE.equals(
                        request.enabled())
                        ? "启用用户成功"
                        : "禁用用户成功");
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('user:delete')")
    public ApiResponse<Void> deleteUsers(
            @RequestParam List<Long> ids,

            @AuthenticationPrincipal
            CurrentUser currentUser) {

        userService.deleteUsers(
                ids,
                currentUser.id());

        return ApiResponse.okMessage(
                "删除用户成功");
    }


}
