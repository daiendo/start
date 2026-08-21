package com.daiend.muriox.datascope;

import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.config.AuthorityCacheProperties;
import com.daiend.muriox.role.DataScopeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserDataScopeService {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    UserDataScopeService.class);

    private static final String CACHE_PREFIX =
            "authority:user:";

    private static final String CACHE_SUFFIX =
            ":data-scope";

    private final DataScopeMapper dataScopeMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthorityCacheProperties cacheProperties;
    private final OrganizeDataScopeVersionService
            versionService;

    public UserDataScopeService(
            DataScopeMapper dataScopeMapper,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AuthorityCacheProperties cacheProperties,
            OrganizeDataScopeVersionService versionService) {

        this.dataScopeMapper = dataScopeMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
        this.versionService = versionService;
    }

    @Transactional(readOnly = true)
    public UserDataScope getDataScope(
            Long userId) {

        validateUserId(userId);

        Long orgVersion =
                versionService.getCurrentVersion();

        if (orgVersion != null) {
            UserDataScope cachedDataScope =
                    readCache(
                            userId,
                            orgVersion);

            if (cachedDataScope != null) {
                return cachedDataScope;
            }
        }

        UserDataScope dataScope =
                calculateDataScope(userId);

        if (orgVersion != null) {
            writeCache(
                    userId,
                    orgVersion,
                    dataScope);
        }

        return dataScope;
    }

    public void evict(Long userId) {
        if (userId == null) {
            return;
        }

        redisTemplate.delete(
                cacheKey(userId));
    }

    private UserDataScope calculateDataScope(
            Long userId) {

        List<UserDataScopeRow> rows =
                dataScopeMapper
                        .findUserDataScopeRows(userId);

        if (rows.isEmpty()) {
            return UserDataScope.denied();
        }

        boolean containsAll =
                rows.stream()
                        .anyMatch(row ->
                                row.dataScopeType()
                                        == DataScopeType.ALL);

        if (containsAll) {
            return UserDataScope.allData();
        }

        Long userOrgId =
                rows.get(0).userOrgId();

        Set<Long> allowedOrgIds =
                new LinkedHashSet<>();

        boolean includeCurrentOrgDescendants =
                false;

        for (UserDataScopeRow row : rows) {
            DataScopeType dataScopeType =
                    row.dataScopeType();

            if (dataScopeType == null) {
                continue;
            }

            switch (dataScopeType) {
                case ALL -> {
                    // 已在循环前短路。
                }

                case CURRENT_ORG -> {
                    if (userOrgId != null) {
                        allowedOrgIds.add(userOrgId);
                    }
                }

                case CURRENT_ORG_AND_CHILDREN -> {
                    if (userOrgId != null) {
                        includeCurrentOrgDescendants =
                                true;
                    }
                }

                case CUSTOM_ORG -> {
                    if (row.customOrgId() != null) {
                        allowedOrgIds.add(
                                row.customOrgId());
                    }
                }
            }
        }

        if (includeCurrentOrgDescendants) {
            allowedOrgIds.addAll(
                    dataScopeMapper
                            .findSelfAndDescendantOrgIds(
                                    userOrgId));
        }

        if (allowedOrgIds.isEmpty()) {
            return UserDataScope.denied();
        }

        return UserDataScope.organizations(
                allowedOrgIds);
    }

    private UserDataScope readCache(
            Long userId,
            long currentOrgVersion) {

        String key = cacheKey(userId);

        try {
            String json =
                    redisTemplate.opsForValue()
                            .get(key);

            if (json == null) {
                return null;
            }

            UserDataScopeCacheEntry entry =
                    objectMapper.readValue(
                            json,
                            UserDataScopeCacheEntry.class);

            if (entry.orgVersion()
                    != currentOrgVersion) {

                return null;
            }

            return entry.toDataScope();
        } catch (Exception exception) {
            LOG.warn(
                    "用户数据范围缓存读取失败，本次回退数据库查询: {}",
                    key,
                    exception);

            try {
                redisTemplate.delete(key);
            } catch (RuntimeException deleteException) {
                LOG.warn(
                        "异常数据范围缓存删除失败: {}",
                        key,
                        deleteException);
            }

            return null;
        }
    }

    private void writeCache(
            Long userId,
            long orgVersion,
            UserDataScope dataScope) {

        String key = cacheKey(userId);

        try {
            UserDataScopeCacheEntry entry =
                    UserDataScopeCacheEntry.from(
                            orgVersion,
                            dataScope);

            String json =
                    objectMapper.writeValueAsString(
                            entry);

            redisTemplate.opsForValue().set(
                    key,
                    json,
                    cacheProperties.ttl());
        } catch (Exception exception) {
            LOG.warn(
                    "用户数据范围缓存写入失败，本次使用数据库计算结果: {}",
                    key,
                    exception);
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(
                    "用户 ID 不合法");
        }
    }

    private String cacheKey(Long userId) {
        return CACHE_PREFIX
                + "{"
                + userId
                + "}"
                + CACHE_SUFFIX;
    }
}