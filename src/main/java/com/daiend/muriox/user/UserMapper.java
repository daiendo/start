package com.daiend.muriox.user;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
                .eq(User::getId, id));
        return Optional.ofNullable(user);
    }

    default List<User> getUsers(String username) {
        LambdaQueryWrapper<User> query = Wrappers.<User>lambdaQuery();
        if (username != null && !username.isBlank()) {
            query.like(
                    User::getUsername,
                    username.trim());
        }

        query.orderByAsc(User::getUsername)
                .orderByAsc(User::getId);

        return selectList(query);
    }
}
