package com.daiend.muriox.auth;

import com.daiend.muriox.datascope.UserDataScopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserAuthorityCacheInvalidator {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    UserAuthorityCacheInvalidator.class);

    private final UserAuthorityService
            userAuthorityService;

    private final UserDataScopeService
            userDataScopeService;

    public UserAuthorityCacheInvalidator(
            UserAuthorityService userAuthorityService,
            UserDataScopeService userDataScopeService) {

        this.userAuthorityService =
                userAuthorityService;

        this.userDataScopeService =
                userDataScopeService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void handleUserAuthorityChangedEvent(
            UserAuthorityChangedEvent event) {

        event.userIds().forEach(userId -> {
            evictAuthorityCache(userId);
            evictDataScopeCache(userId);
        });
    }

    private void evictAuthorityCache(Long userId) {
        try {
            userAuthorityService.evict(userId);
        } catch (RuntimeException exception) {
            LOG.error(
                    "用户菜单和按钮权限缓存删除失败，将在缓存过期后重新加载: {}",
                    userId,
                    exception);
        }
    }

    private void evictDataScopeCache(Long userId) {
        try {
            userDataScopeService.evict(userId);
        } catch (RuntimeException exception) {
            LOG.error(
                    "用户数据范围缓存删除失败，将在缓存过期后重新加载: {}",
                    userId,
                    exception);
        }
    }
}