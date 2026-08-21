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

    UserDetailRow selectUserDetail(
            @Param("id") Long id);

    List<UserPostRow> selectUserPosts(
            @Param("userId") Long userId);


    default boolean existsByAccountExcludingId(
            String account,
            Long excludedId) {

        return selectCount(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getAccount, account)
                        .ne(User::getId, excludedId)
        ) > 0;
    }

    default boolean existsByEmailExcludingId(
            String email,
            Long excludedId) {

        return selectCount(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getEmail, email)
                        .ne(User::getId, excludedId)
        ) > 0;
    }

    default boolean existsByMobileExcludingId(
            String mobile,
            Long excludedId) {

        return selectCount(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getMobile, mobile)
                        .ne(User::getId, excludedId)
        ) > 0;
    }

    int deleteUserPosts(
            @Param("userId") Long userId);

    List<Long> findPostIdsByUserId(
            @Param("userId") Long userId);

    boolean existsEnabledRoleByUserId(
            @Param("userId") Long userId);

    int deleteUserPostsByUserIds(
            @Param("userIds")
            Collection<Long> userIds);

    int deleteUserRolesByUserIds(
            @Param("userIds")
            Collection<Long> userIds);

    List<Long> findEnabledUserIdsWithoutEnabledRole(
            @Param("userIds")
            Collection<Long> userIds);

}
