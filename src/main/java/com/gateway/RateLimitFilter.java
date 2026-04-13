package com.gateway;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@WebFilter(urlPatterns = "/*")
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final Map<String, UserRequestInfo> requestCounts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String ipAddress = httpRequest.getRemoteAddr();

        long currentTime = System.currentTimeMillis() / 60000;

        UserRequestInfo info = requestCounts.compute(ipAddress, (k, v) -> {
            if (v == null || v.minute != currentTime) {
                return new UserRequestInfo(currentTime, 1);
            }
            v.count.incrementAndGet();
            return v;
        });

        if (info.count.get() > MAX_REQUESTS_PER_MINUTE) {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Rate limit exceeded. Try again in a minute.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private static class UserRequestInfo {
        long minute;
        AtomicInteger count;

        UserRequestInfo(long minute, int count) {
            this.minute = minute;
            this.count = new AtomicInteger(count);
        }
    }
}
