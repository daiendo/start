package com.daiend.muriox.organize;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class OrganizeHierarchyChangePublisher {

    private final ApplicationEventPublisher eventPublisher;

    public OrganizeHierarchyChangePublisher(
            ApplicationEventPublisher eventPublisher) {

        this.eventPublisher = eventPublisher;
    }

    public void publish() {
        eventPublisher.publishEvent(
                new OrganizeHierarchyChangedEvent());
    }
}