package com.bibei.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpaFallbackFilter extends OncePerRequestFilter {
    private static final String SPA_ENTRYPOINT = "/index.html";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (shouldForward(request)) {
            request.getRequestDispatcher(SPA_ENTRYPOINT).forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldForward(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isBackendPath(path) || hasFileExtension(path)) {
            return false;
        }
        return true;
    }

    private boolean isBackendPath(String path) {
        return path.equals("/api")
                || path.startsWith("/api/")
                || path.equals("/healthz")
                || path.equals("/error");
    }

    private boolean hasFileExtension(String path) {
        int lastSlash = path.lastIndexOf('/');
        String lastSegment = path.substring(lastSlash + 1);
        return lastSegment.contains(".");
    }
}
