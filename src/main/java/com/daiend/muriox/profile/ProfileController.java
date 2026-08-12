package com.daiend.muriox.profile;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.daiend.muriox.auth.CurrentUser;
import com.daiend.muriox.common.ApiResponse;

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
        return ApiResponse.okMessage("修改成功");
    }
}
