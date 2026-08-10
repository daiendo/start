package com.daiend.muriox.dict;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DictMapper {

    @Select("""
            SELECT item.label
            FROM sys_dict dict
            JOIN sys_dict_item item
                ON item.dict_id = dict.id
            WHERE LOWER(dict.code) = LOWER(#{dictCode})
              AND item.value = #{value}
              AND dict.status = TRUE
              AND item.status = TRUE
            """)
    String selectLabel(
            @Param("dictCode") String dictCode,
            @Param("value") String value);
}