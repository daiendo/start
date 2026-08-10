package com.daiend.muriox.profile;

import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.dict.DictService;
import com.daiend.muriox.user.User;
import com.daiend.muriox.user.UserMapper;
import com.daiend.muriox.user.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {
    private static final String USER_GENDER_DICT_CODE =  "user_gender";
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final DictService dictService;


    public ProfileService(UserMapper userMapper, PasswordEncoder passwordEncoder, DictService dictService){
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.dictService = dictService;
    }


    public ProfileResponse profile(Long id) {
        User user = userMapper.findById(id).orElseThrow(()->new BusinessException("用户不存在"));
        String sexLabel = dictService
                .findLabel(
                        USER_GENDER_DICT_CODE,
                        user.getSex())
                .orElse("未知");
        UserResponse userResponse = new UserResponse(
                id,
                user.getAccount(),
                user.getUsername(),
                user.getAvatar(),
                user.getEmail(),
                user.getMobile(),
                user.getSex(),
                sexLabel,
                user.getRemark());
        return  new ProfileResponse(userResponse, List.of(),List.of());
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
