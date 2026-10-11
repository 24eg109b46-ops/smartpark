package com.smartpark.security;

import com.smartpark.entity.User;
import com.smartpark.enums.Role;
import com.smartpark.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuditLogService auditLogService;

    public CustomAuthenticationSuccessHandler(@Lazy AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            User user = userDetails.getUser();
            auditLogService.log(user.getId(), user.getEmail(), "LOGIN", "User logged in successfully", request.getRemoteAddr());
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals(Role.ROLE_ADMIN.name())) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return;
            } else if (role.equals(Role.ROLE_OPERATOR.name())) {
                response.sendRedirect(request.getContextPath() + "/operator/dashboard");
                return;
            } else if (role.equals(Role.ROLE_DRIVER.name())) {
                response.sendRedirect(request.getContextPath() + "/driver/dashboard");
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/");
    }
}
