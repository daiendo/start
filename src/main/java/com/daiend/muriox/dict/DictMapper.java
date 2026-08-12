package com.daiend.muriox.dict;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    @Select("""
        SELECT item.id,
               item.label,
               item.value,
               dict.value_type,
               item.extra ->> 'tagType' AS tag_type
        FROM sys_dict dict
        JOIN sys_dict_item item
            ON item.dict_id = dict.id
        WHERE LOWER(dict.code) = LOWER(#{code})
          AND dict.status = TRUE
          AND item.status = TRUE
        ORDER BY item.sort_order,
                 item.id
        """)
    List<DictOptionRow> selectOptionsByCode(
            @Param("code") String code);

}