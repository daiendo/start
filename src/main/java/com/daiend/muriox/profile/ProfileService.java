package com.daiend.muriox.profile;

import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.user.User;
import com.daiend.muriox.user.UserMapper;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    public ProfileService(UserMapper userMapper, PasswordEncoder passwordEncoder){
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }


    public ProfileResponse profile(Long id) {
        User user = userMapper.findById(id).orElseThrow(()->new BusinessException("用户不存在"));
        ProfileUserResponse profileUser = new ProfileUserResponse(id,user.getAccount(),user.getUsername(),user.getEmail(),user.getRemark());
        return  new ProfileResponse(profileUser, List.of(),List.of());
    }
    public void changePassword(Long id,ChangePassRequest changePassRequest){
        User user = userMapper.findById(id).orElseThrow(()->new BusinessException("用户不存在"));
        if (!passwordEncoder.matches(changePassRequest.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException("旧密码错误");
        }
        if(!changePassRequest.newPassword().equals(changePassRequest.confirmPassword())){
            throw new BusinessException("新密码与确认密码不一致");
        }
        if (passwordEncoder.matches(
                changePassRequest.newPassword(),
                user.getPasswordHash())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        User passwordUpdate = new User();
        passwordUpdate.setId(id);
        passwordUpdate.setPasswordHash(
                passwordEncoder.encode(changePassRequest.newPassword()));

        int affectedRows = userMapper.updateById(passwordUpdate);

        if (affectedRows != 1) {
            throw new BusinessException("密码修改失败");
        }
    }
}
