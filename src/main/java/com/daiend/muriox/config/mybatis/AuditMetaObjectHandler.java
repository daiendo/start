package com.daiend.muriox.config.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();

        strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, now);
    }
}
