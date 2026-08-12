package com.daiend.muriox.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserAuthorityCacheInvalidator {
    private static final Logger LOG = LoggerFactory.getLogger(UserAuthorityCacheInvalidator.class);
    private final UserAuthorityService userAuthorityService;

    public UserAuthorityCacheInvalidator(UserAuthorityService userAuthorityService) {
        this.userAuthorityService = userAuthorityService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserAuthorityChangedEvent(UserAuthorityChangedEvent event) {
        event.userIds().forEach(userId -> {
            try {
                userAuthorityService.evict(userId);
            } catch (RuntimeException exception) {
                LOG.error("用户权限缓存删除失败，将在缓存过期后重新加载: {}",
                        userId,
                        exception);
            }
        });
    }
}
