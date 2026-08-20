package com.daiend.muriox.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    default boolean existsByAccount(String account) {
        return selectCount(Wrappers.<User>lambdaQuery().eq(User::getAccount, account)) > 0;
    }

    default boolean existsByEmail(String email) {
        return selectCount(Wrappers.<User>lambdaQuery().eq(User::getEmail, email)) > 0;
    }

    default boolean existsByMobile(String mobile) {
        return selectCount(Wrappers.<User>lambdaQuery().eq(User::getMobile, mobile)) > 0;
    }

    Page<UserPageRow> selectUserPage(
            Page<UserPageRow> page,

            @Param("account")
            String account,

            @Param("orgId")
            Long orgId);

    int insertUserPosts(
            @Param("userId") Long userId,
            @Param("postIds") Collection<Long> postIds);


}
