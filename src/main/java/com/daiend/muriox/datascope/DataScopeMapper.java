package com.daiend.muriox.datascope;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataScopeMapper {

    List<UserDataScopeRow> findUserDataScopeRows(
            @Param("userId") Long userId);

    List<Long> findSelfAndDescendantOrgIds(
            @Param("orgId") Long orgId);
}