package com.daiend.muriox.organize;

import com.daiend.muriox.datascope.OrganizeDataScopeVersionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrganizeHierarchyVersionListener {

    private final OrganizeDataScopeVersionService
            versionService;

    public OrganizeHierarchyVersionListener(
            OrganizeDataScopeVersionService versionService) {

        this.versionService = versionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void handle(
            OrganizeHierarchyChangedEvent event) {

        versionService.incrementVersion();
    }
}