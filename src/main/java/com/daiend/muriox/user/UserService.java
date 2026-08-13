package com.daiend.muriox.user;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;


    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public List<UserListResponse> getUsers(
            String username) {

        return userMapper.getUsers(username)
                .stream()
                .map(this::toUserListResponse)
                .toList();
    }


    private UserListResponse toUserListResponse(User user) {
        return new UserListResponse(
                user.getId(),
                user.getAccount(),
                user.getUsername(),
                user.getEnabled());
    }

}
