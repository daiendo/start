package com.daiend.muriox.datascope;

import org.springframework.security.access.AccessDeniedException;

public class DataScopeAccessDeniedException
        extends AccessDeniedException {

    public DataScopeAccessDeniedException(String message) {
        super(message);
    }
}
