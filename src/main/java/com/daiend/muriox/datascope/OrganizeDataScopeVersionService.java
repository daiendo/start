package com.daiend.muriox.datascope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrganizeDataScopeVersionService {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    OrganizeDataScopeVersionService.class);

    private static final String VERSION_KEY =
            "authority:data-scope:org-version";

    private final StringRedisTemplate redisTemplate;

    public OrganizeDataScopeVersionService(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    /*
     * 返回 null 表示 Redis 不可用。
     * 调用方必须绕过缓存并查询数据库。
     */
    public Long getCurrentVersion() {
        try {
            String value =
                    redisTemplate.opsForValue()
                            .get(VERSION_KEY);

            return value == null
                    ? 0L
                    : Long.valueOf(value);
        } catch (RuntimeException exception) {
            LOG.warn(
                    "读取组织数据范围版本失败，本次绕过数据范围缓存",
                    exception);

            return null;
        }
    }

    public void incrementVersion() {
        try {
            redisTemplate.opsForValue()
                    .increment(VERSION_KEY);
        } catch (RuntimeException exception) {
            /*
             * 版本递增失败时旧缓存最多存活到 TTL。
             * 不能因此回滚已经提交的组织事务。
             */
            LOG.error(
                    "组织数据范围版本递增失败，旧缓存将在过期后刷新",
                    exception);
        }
    }
}