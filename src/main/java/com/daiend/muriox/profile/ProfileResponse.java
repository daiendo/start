package com.daiend.muriox.profile;

import com.daiend.muriox.menu.response.MenuNode;
import com.daiend.muriox.user.UserResponse;

import java.util.List;

public record ProfileResponse(
        UserResponse user,
        List<String> permissionsList,
        List<MenuNode>  menus
) {
    
}
