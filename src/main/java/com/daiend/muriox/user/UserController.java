package com.daiend.muriox.user;

import com.daiend.muriox.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/authority/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public ApiResponse<List<UserListResponse>> getUsers(@RequestParam(required = false) String username) {

        return ApiResponse.ok(userService.getUsers(username));

    }
}
