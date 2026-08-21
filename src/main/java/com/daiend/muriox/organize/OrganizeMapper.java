package com.daiend.muriox.organize;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface OrganizeMapper extends BaseMapper<Organize> {

    List<Organize> findAllowedAndAncestorOrganizes(
            @Param("orgIds") Collection<Long> orgIds);

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

    boolean existsByNameAndParentId(
            @Param("name") String name,
            @Param("parentId") Long parentId);

    boolean existsByNameAndParentIdExcludingId(
            @Param("name") String name,
            @Param("parentId") Long parentId,
            @Param("excludedId") Long excludedId);

    boolean hasChildrenByParentIds(
            @Param("organizeIds")
            Collection<Long> organizeIds);
}
