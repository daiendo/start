package com.daiend.muriox.post;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    default boolean existsByOrgIds(Collection<Long> orgIds) {
        return selectCount(
                Wrappers.<Post>lambdaQuery()
                        .in(Post::getOrgId, orgIds)
        ) > 0;
    }

    default Page<Post> selectPostPage(long current, long size, String name, Long orgId) {
        Page<Post> page = new Page<>(current, size);
        LambdaQueryWrapper<Post> wrapper = Wrappers.<Post>lambdaQuery();
        if (name != null && !name.isBlank()) {
            wrapper.like(Post::getName, name.trim());
        }
        if (orgId != null && orgId > 0) {
            wrapper.eq(Post::getOrgId, orgId);
        }
        wrapper.orderByAsc(Post::getSortOrder)
                .orderByAsc(Post::getId);
        return selectPage(page, wrapper);
    }

    default boolean existsByOrgIdAndCode(
            Long orgId,
            String code) {

        return selectCount(
                Wrappers.<Post>lambdaQuery()
                        .eq(Post::getOrgId, orgId)
                        .eq(Post::getCode, code)
        ) > 0;
    }

    default boolean existsByOrgIdAndName(
            Long orgId,
            String name) {

        return selectCount(
                Wrappers.<Post>lambdaQuery()
                        .eq(Post::getOrgId, orgId)
                        .eq(Post::getName, name)
        ) > 0;
    }

    default boolean existsByOrgIdAndCodeExcludingId(
            Long orgId, String code, Long excludedId) {
        return selectCount(
                Wrappers.<Post>lambdaQuery()
                        .eq(Post::getOrgId, orgId)
                        .eq(Post::getCode, code)
                        .ne(Post::getId, excludedId)
        ) > 0;
    }

    default boolean existsByOrgIdAndNameExcludingId(
            Long orgId, String name, Long excludedId) {
        return selectCount(
                Wrappers.<Post>lambdaQuery()
                        .eq(Post::getOrgId, orgId)
                        .eq(Post::getName, name)
                        .ne(Post::getId, excludedId)
        ) > 0;
    }
}
