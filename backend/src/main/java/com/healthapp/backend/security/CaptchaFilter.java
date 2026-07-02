package com.healthapp.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class CaptchaFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Only inspect POST requests to the login endpoint
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getRequestURI())) {
            String sessionCaptcha = (String) request.getSession().getAttribute("captcha");
            String requestCaptcha = request.getParameter("captcha");

            if (sessionCaptcha == null || requestCaptcha == null || !sessionCaptcha.equalsIgnoreCase(requestCaptcha.trim())) {
                // Captcha validation failed, redirect to login with error indicator
                String role = request.getParameter("role");
                String redirectUrl = "/login?captchaError=true";
                if (role != null && !role.trim().isEmpty()) {
                    redirectUrl += "&role=" + role;
                }
                response.sendRedirect(redirectUrl);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
