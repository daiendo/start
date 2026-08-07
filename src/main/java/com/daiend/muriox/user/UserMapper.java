package com.daiend.muriox.user;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    default Optional<User> findByAccount(String account) {
        User user = selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getAccount, account));
        return Optional.ofNullable(user);
    }

    default Optional<User> findById(Long id) {
        User user = selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getId,id));
        return Optional.ofNullable(user);
    }
}
