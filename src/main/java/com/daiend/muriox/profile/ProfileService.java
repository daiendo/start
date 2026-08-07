package com.daiend.muriox.profile;

import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.user.User;
import com.daiend.muriox.user.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {
    private final UserMapper userMapper;

    public ProfileService(UserMapper userMapper){
        this.userMapper = userMapper;
    }


    public ProfileResponse profile(Long id) {
        User user = userMapper.findById(id).orElseThrow(()->new BusinessException("用户不存在"));
        ProfileUserResponse profileUser = new ProfileUserResponse(id,user.getAccount(),user.getUsername());
        return  new ProfileResponse(profileUser, List.of(),List.of());
    }
}
