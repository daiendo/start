package com.daiend.muriox.role;

import com.daiend.muriox.common.ApiResponse;
import com.daiend.muriox.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authority/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<RoleResponse>> page(
            @RequestParam(defaultValue = "1")
            long current,

            @RequestParam(defaultValue = "10")
            long size,

            @RequestParam(required = false)
            String name) {

        return ApiResponse.ok(
                roleService.page(
                        current,
                        size,
                        name));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:add')")
    public ApiResponse<Long> create(
            @Valid @RequestBody RoleRequest request) {

        return ApiResponse.ok(
                roleService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(roleService.getRole(id));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('role:edit')")
    public ApiResponse<Long> update(@Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.ok(roleService.update(request));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('role:delete')")
    public ApiResponse<Void> deleteRoles(
            @RequestParam List<Long> ids) {

        roleService.deleteRoles(ids);

        return ApiResponse.okMessage(
                "删除角色成功");
    }

    @GetMapping("/authority/{roleId}")
    @PreAuthorize(
            "hasAuthority('role:config:authority')")
    public ApiResponse<RoleAuthorityResponse> getAuthority(
            @PathVariable Long roleId) {

        return ApiResponse.ok(
                roleService.getAuthority(roleId));
    }

    @PostMapping("/authority")
    @PreAuthorize(
            "hasAuthority('role:config:authority')")
    public ApiResponse<Void> configureAuthority(
            @Valid @RequestBody
            RoleAuthorityRequest request) {

        roleService.configureAuthority(request);

        return ApiResponse.okMessage(
                "配置角色权限成功");
    }


    @GetMapping("/user/{roleId}")
    public ApiResponse<List<Long>> getUsers(@PathVariable Long roleId) {
        return ApiResponse.ok(roleService.getUsers(roleId));

    }

    @PostMapping("/user")
    @PreAuthorize(
            "hasAuthority('role:user:authorization')")
    public ApiResponse<Void> configureUsers(
            @Valid @RequestBody
            RoleUserRequest request) {

        roleService.configureUsers(request);

        return ApiResponse.okMessage(
                "角色用户授权成功");
    }
}
