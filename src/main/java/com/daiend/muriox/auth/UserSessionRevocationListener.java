package com.daiend.muriox.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserSessionRevocationListener {
    private static final Logger LOG =
            LoggerFactory.getLogger(
                    UserSessionRevocationListener.class);

    private final TokenService tokenService;

    public UserSessionRevocationListener(
            TokenService tokenService) {

        this.tokenService = tokenService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void handle(
            UserSessionsRevocationRequestedEvent event) {

        event.userIds().forEach(userId -> {
            try {
                tokenService.revokeAllSessions(
                        userId);
            } catch (RuntimeException exception) {
                LOG.error(
                        "撤销用户全部会话失败，userId={}",
                        userId,
                        exception);
            }
        });
    }
}