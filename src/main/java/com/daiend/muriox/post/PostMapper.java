package com.daiend.muriox.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
}
