package com.daiend.muriox.menu;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
    default Page<Menu> selectRootPage(long current, long size) {
        Page<Menu> page = new Page<>(current, size);

        return selectPage(
                page,
                Wrappers.<Menu>lambdaQuery()
                        .isNull(Menu::getParentId)
                        .orderByAsc(Menu::getSortOrder)
                        .orderByAsc(Menu::getId)
        );
    }
}
