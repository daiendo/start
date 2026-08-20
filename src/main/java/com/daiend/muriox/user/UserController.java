package com.daiend.muriox.user;

import com.daiend.muriox.common.ApiResponse;
import com.daiend.muriox.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/authority/user")
public class UserController {

    private final UserService userService;
    private final AvatarService avatarService;

    public UserController(UserService userService, AvatarService avatarService) {
        this.userService = userService;
        this.avatarService = avatarService;
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
}
