package com.daiend.muriox.auth;

import com.daiend.muriox.config.AuthorityCacheProperties;
import com.daiend.muriox.menu.MenuService;
import com.daiend.muriox.menu.response.MenuNode;
import com.daiend.muriox.resource.ResourceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Service
public class UserAuthorityService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserAuthorityService.class);

    private static final String CACHE_PREFIX =
            "authority:user:";

    private static final String PERMISSIONS_SUFFIX =
            ":permissions";

    private static final String MENUS_SUFFIX =
            ":menus";

    private final ResourceMapper resourceMapper;
    private final MenuService menuService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthorityCacheProperties cacheProperties;

    public UserAuthorityService(
            ResourceMapper resourceMapper,
            MenuService menuService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AuthorityCacheProperties cacheProperties) {
        this.resourceMapper = resourceMapper;
        this.menuService = menuService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
    }

    public List<String> findPermissionCodes(Long userId) {
        String cacheKey = permissionsKey(userId);

        String[] cachedCodes =
                readCache(cacheKey, String[].class);

        if (cachedCodes != null) {
            return normalizePermissionCodes(
                    Arrays.asList(cachedCodes));
        }

        List<String> permissionCodes =
                normalizePermissionCodes(
                        resourceMapper
                                .findEnabledCodesByUserId(userId));

        writeCache(cacheKey, permissionCodes);

        return permissionCodes;
    }

    public List<MenuNode> findMenus(Long userId) {
        String cacheKey = menusKey(userId);

        MenuNode[] cachedMenus =
                readCache(cacheKey, MenuNode[].class);

        if (cachedMenus != null) {
            return List.copyOf(
                    Arrays.asList(cachedMenus));
        }

        List<MenuNode> menus =
                menuService
                        .findEnabledMenuTreeByUserId(userId);

        writeCache(cacheKey, menus);

        return menus;
    }

    public void evict(Long userId) {
        redisTemplate.delete(List.of(
                permissionsKey(userId),
                menusKey(userId)));
    }

    public void evictPermissions(Long userId) {
        redisTemplate.delete(
                permissionsKey(userId));
    }

    public void evictMenus(Long userId) {
        redisTemplate.delete(
                menusKey(userId));
    }

    private List<String> normalizePermissionCodes(
            List<String> permissionCodes) {

        return permissionCodes.stream()
                .filter(code ->
                        code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private <T> T readCache(
            String cacheKey,
            Class<T> valueType) {

        try {
            String cachedValue =
                    redisTemplate.opsForValue().get(cacheKey);

            if (cachedValue == null) {
                return null;
            }

            return objectMapper.readValue(
                    cachedValue,
                    valueType);
        } catch (Exception exception) {
            LOGGER.warn(
                    "权限缓存读取失败，本次请求回退数据库查询: {}",
                    cacheKey,
                    exception);

            try {
                redisTemplate.delete(cacheKey);
            } catch (Exception deleteException) {
                LOGGER.warn(
                        "异常权限缓存删除失败: {}",
                        cacheKey,
                        deleteException);
            }

            return null;
        }
    }

    private void writeCache(
            String cacheKey,
            Object value) {

        try {
            String json =
                    objectMapper.writeValueAsString(value);

            redisTemplate.opsForValue().set(
                    cacheKey,
                    json,
                    cacheProperties.ttl());
        } catch (Exception exception) {
            LOGGER.warn(
                    "权限缓存写入失败，本次请求使用数据库查询结果: {}",
                    cacheKey,
                    exception);
        }
    }

    private String permissionsKey(Long userId) {
        return CACHE_PREFIX
                + "{"
                + userId
                + "}"
                + PERMISSIONS_SUFFIX;
    }

    private String menusKey(Long userId) {
        return CACHE_PREFIX
                + "{"
                + userId
                + "}"
                + MENUS_SUFFIX;
    }
}