package com.gateway;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(urlPatterns = "/*")
public class LoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        long startTime = System.currentTimeMillis();

        String ipAddress = httpRequest.getRemoteAddr();
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();

        MetricsManager.incrementTotalRequests();
        MetricsManager.incrementRouteHit(uri);
        System.out.println(">>> INCOMING REQUEST: [" + method + "] " + uri + " from " + ipAddress);

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            if (httpResponse.getStatus() >= 400) {
                MetricsManager.incrementFailedRequests();
            }
            System.out.println("<<< RESPONSE SENT: [" + method + "] " + uri + " - Status: " + httpResponse.getStatus() + " (" + duration + "ms)");
        }
    }
}
