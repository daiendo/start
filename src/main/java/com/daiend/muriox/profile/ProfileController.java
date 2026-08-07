package com.daiend.muriox.profile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daiend.muriox.auth.CurrentUser;
import com.daiend.muriox.common.ApiResponse;

@RestController
@RequestMapping("/api/authority")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(
            @AuthenticationPrincipal CurrentUser currentUser) {
                return ApiResponse.ok(profileService.getProfile(currentUser.id()));
    }

}
