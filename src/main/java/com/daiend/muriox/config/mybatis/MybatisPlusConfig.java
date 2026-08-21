package com.daiend.muriox.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.daiend.muriox.datascope.MurioxDataPermissionHandler;
import com.daiend.muriox.datascope.SelectDataPermissionInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            MurioxDataPermissionHandler dataPermissionHandler) {
        MybatisPlusInterceptor interceptor =
                new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination =
                new PaginationInnerInterceptor(DbType.POSTGRE_SQL);

        pagination.setOverflow(false);
        pagination.setMaxLimit(100L);

        interceptor.addInnerInterceptor(
                new SelectDataPermissionInterceptor(
                        dataPermissionHandler));
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
