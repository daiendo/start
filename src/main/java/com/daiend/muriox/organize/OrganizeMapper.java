package com.daiend.muriox.organize;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface OrganizeMapper extends BaseMapper<Organize> {

    default Page<Organize> selectRootOrganizePage(long current, long size) {
        Page<Organize> page = new Page<>(current, size);
        return selectPage(page,
                Wrappers.<Organize>lambdaQuery()
                        .isNull(Organize::getParentId)
                        .orderByAsc(Organize::getSortOrder)
                        .orderByAsc(Organize::getId));
    }

    default List<Organize> findAllOrganize() {
        return selectList(Wrappers.<Organize>lambdaQuery()
                .orderByAsc(Organize::getParentId)
                .orderByAsc(Organize::getSortOrder)
                .orderByAsc(Organize::getId)

        );
    }

    default boolean existsByNameAndParentId(
            String name,
            Long parentId) {

        LambdaQueryWrapper<Organize> query = Wrappers.<Organize>lambdaQuery()
                .eq(Organize::getName, name);

        if (parentId == null) {
            query.isNull(Organize::getParentId);
        } else {
            query.eq(Organize::getParentId, parentId);
        }

        return selectCount(query) > 0;
    }


    default boolean existsByNameAndParentIdExcludingId(String name,
                                                       Long parentId,
                                                       Long excludedId) {
        LambdaQueryWrapper<Organize> query =
                Wrappers.<Organize>lambdaQuery()
                        .eq(Organize::getName, name)
                        .ne(Organize::getId, excludedId);

        if (parentId == null) {
            query.isNull(Organize::getParentId);
        } else {
            query.eq(Organize::getParentId, parentId);
        }

        return selectCount(query) > 0;
    }

    default boolean hasChildrenByParentIds(Collection<Long> organizeIds) {
        return selectCount(
                Wrappers.<Organize>lambdaQuery()
                        .in(Organize::getParentId, organizeIds)
        ) > 0;
    }
}
