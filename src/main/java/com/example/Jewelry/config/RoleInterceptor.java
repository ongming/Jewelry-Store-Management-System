package com.example.Jewelry.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        if (!uri.startsWith("/admin") && !uri.startsWith("/staff")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        String roleName = session == null ? null : (String) session.getAttribute("roleName");

        if (roleName == null || roleName.isBlank()) {
            response.sendRedirect("/login");
            return false;
        }

        if (uri.startsWith("/admin") && !"ADMIN".equalsIgnoreCase(roleName)) {
            response.sendRedirect("/staff/dashboard");
            return false;
        }

        if (uri.startsWith("/staff")
            && !("STAFF".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName))) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}

