package com.example.Jewelry.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    private static final String LOGIN_URL = "/auth/login";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        if (!isProtectedPath(uri)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        String roleName = session == null ? null : (String) session.getAttribute("roleName");

        if (roleName == null || roleName.isBlank()) {
            response.sendRedirect(LOGIN_URL);
            return false;
        }

        if (isAdminOnlyPath(uri) && !"ADMIN".equalsIgnoreCase(roleName)) {
            response.sendRedirect("/staff/dashboard");
            return false;
        }

        if (isStaffOrAdminPath(uri)
            && !("STAFF".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName))) {
            response.sendRedirect(LOGIN_URL);
            return false;
        }

        return true;
    }

    private boolean isProtectedPath(String uri) {
        return isAdminOnlyPath(uri) || isStaffOrAdminPath(uri);
    }

    private boolean isAdminOnlyPath(String uri) {
        return uri.startsWith("/dashboard/admin")
            || uri.startsWith("/products/manage")
            || uri.startsWith("/products/categories")
            || uri.startsWith("/auth/accounts")
            || uri.startsWith("/admin/suppliers")
            || uri.startsWith("/admin");
    }

    private boolean isStaffOrAdminPath(String uri) {
        return uri.equals("/dashboard")
            || uri.startsWith("/dashboard/staff")
            || uri.startsWith("/products/staff")
            || uri.startsWith("/orders")
            || uri.startsWith("/customers")
            || uri.startsWith("/pos")
            || uri.startsWith("/staff");
    }
}