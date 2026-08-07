package com.daiend.muriox.profile;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {
    public ProfileResponse getProfile(Long id) {
        ProfileUserResponse user = new ProfileUserResponse(id,"muriox","muriox");
        return  new ProfileResponse(user, List.of(),List.of());
    }
}
