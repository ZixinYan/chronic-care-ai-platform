package com.zixin.accountprovider.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {

        String traceId = request.getHeader("X-Trace-Id");
        if (traceId != null) {
            MDC.put("X-Trace-Id", traceId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("X-Trace-Id");
        }
    }
}
