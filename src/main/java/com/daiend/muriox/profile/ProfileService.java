package com.daiend.muriox.profile;

import com.daiend.muriox.auth.UserAuthorityService;
import com.daiend.muriox.auth.UserSessionRevocationPublisher;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.dict.DictService;
import com.daiend.muriox.menu.response.MenuNode;
import com.daiend.muriox.user.User;
import com.daiend.muriox.user.UserMapper;
import com.daiend.muriox.user.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProfileService {
    private static final String USER_GENDER_DICT_CODE = "user_gender";
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final DictService dictService;
    private final UserAuthorityService userAuthorityService;
    private final UserSessionRevocationPublisher sessionRevocationPublisher;

    public ProfileService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            DictService dictService,
            UserAuthorityService userAuthorityService,
            UserSessionRevocationPublisher sessionRevocationPublisher) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.dictService = dictService;
        this.userAuthorityService = userAuthorityService;
        this.sessionRevocationPublisher = sessionRevocationPublisher;
    }


    public ProfileResponse profile(Long id) {
        User user = userMapper.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
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
        List<String> permissionsList =
                userAuthorityService.findPermissionCodes(id);

        List<MenuNode> menus =
                userAuthorityService.findMenus(id);

        return new ProfileResponse(userResponse, permissionsList, menus);
    }

    @Transactional
    public void changePassword(
            Long id,
            ChangePassRequest request) {

        User user =
                userMapper.findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "用户不存在"));

        if (!passwordEncoder.matches(
                request.oldPassword(),
                user.getPasswordHash())) {

            throw new BusinessException(
                    "旧密码错误");
        }

        if (!request.newPassword().equals(
                request.confirmPassword())) {

            throw new BusinessException(
                    "新密码与确认密码不一致");
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash())) {

            throw new BusinessException(
                    "新密码不能与旧密码相同");
        }

        User passwordUpdate = new User();
        passwordUpdate.setId(id);
        passwordUpdate.setPasswordHash(
                passwordEncoder.encode(
                        request.newPassword()));
        passwordUpdate.setMustChangePassword(false);
        passwordUpdate.setPasswordChangedAt(
                LocalDateTime.now());

        int affectedRows =
                userMapper.updateById(
                        passwordUpdate);

        if (affectedRows != 1) {
            throw new BusinessException(
                    "密码修改失败");
        }

        /*
         * 提交后撤销当前用户的全部 Session，
         * 包括本次使用临时密码创建的 Session。
         */
        sessionRevocationPublisher.publishForUser(
                user.getId());
    }
}
