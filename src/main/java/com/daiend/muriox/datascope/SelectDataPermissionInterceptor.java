package com.daiend.muriox.datascope;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import org.apache.ibatis.executor.statement.StatementHandler;

import java.sql.Connection;

/**
 * 数据权限当前只负责 SELECT 查询。
 *
 * 写操作由各业务服务在读取原数据后显式校验，避免 UPDATE / DELETE
 * 被 SQL 改写成零影响行后无法区分“不存在”和“越权”。
 */
public class SelectDataPermissionInterceptor
        extends DataPermissionInterceptor {

    public SelectDataPermissionInterceptor(
            MultiDataPermissionHandler dataPermissionHandler) {

        super(dataPermissionHandler);
    }

    @Override
    public void beforePrepare(
            StatementHandler statementHandler,
            Connection connection,
            Integer transactionTimeout) {
        // 写操作统一由 DataScopeGuard 显式校验。
    }
}
