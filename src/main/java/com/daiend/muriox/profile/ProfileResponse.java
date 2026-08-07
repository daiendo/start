package com.daiend.muriox.profile;

import java.util.List;

public record ProfileResponse(
        ProfileUserResponse user,
        List<String> permissionsList,
        List<MenuNodeResponse>  menus
) {
    
}
