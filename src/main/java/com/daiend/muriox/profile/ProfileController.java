package com.daiend.muriox.profile;

import com.daiend.muriox.auth.CurrentUser;
import com.daiend.muriox.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/authority/profile")
public class ProfileController {
    private final ProfileService profileService;


    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;

    }

    @GetMapping("/info")
    public ApiResponse<ProfileResponse> profile(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(profileService.profile(currentUser.id()));
    }

    @PostMapping("/changePassword")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ChangePassRequest changePassRequest
    ) {
        profileService.changePassword(currentUser.id(), changePassRequest);
        return ApiResponse.okMessage(
                "密码修改成功，请重新登录");
    }
}
