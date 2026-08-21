package com.daiend.muriox.security;

import com.daiend.muriox.auth.CurrentUser;
import com.daiend.muriox.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PasswordChangeRequiredFilter
        extends OncePerRequestFilter {

    private static final String CHANGE_PASSWORD_PATH =
            "/api/authority/profile/changePassword";

    private static final String LOGOUT_PATH =
            "/api/authority/auth/logout";

    private final ObjectMapper objectMapper;

    public PasswordChangeRequiredFilter(
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        Object principal =
                authentication == null
                        ? null
                        : authentication.getPrincipal();

        if (principal instanceof CurrentUser currentUser
                && currentUser.mustChangePassword()
                && !isAllowedRequest(request)) {

            writePasswordChangeRequired(
                    response);

            return;
        }

        filterChain.doFilter(
                request,
                response);
    }

    private boolean isAllowedRequest(
            HttpServletRequest request) {

        if (!"POST".equalsIgnoreCase(
                request.getMethod())) {

            return false;
        }

        String servletPath =
                request.getServletPath();

        return CHANGE_PASSWORD_PATH.equals(
                servletPath)
                || LOGOUT_PATH.equals(
                servletPath);
    }

    private void writePasswordChangeRequired(
            HttpServletResponse response)
            throws IOException {

        response.setStatus(
                HttpStatus.FORBIDDEN.value());

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE);

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name());

        ApiResponse<Void> body =
                ApiResponse.fail(
                        HttpStatus.FORBIDDEN.value(),
                        "请先修改初始密码");

        objectMapper.writeValue(
                response.getOutputStream(),
                body);
    }
}