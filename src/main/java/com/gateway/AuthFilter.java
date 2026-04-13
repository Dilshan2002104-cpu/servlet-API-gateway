package com.gateway;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(urlPatterns = "/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    private static final String VALID_API_KEY = "my-secret-key-123";
    private static final String JWT_SECRET = "your-very-secure-secret-key-that-is-long-enough-for-hs256";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String apiKey = httpRequest.getHeader("X-API-Key");
        String authHeader = httpRequest.getHeader("Authorization");

        boolean isAuthenticated = false;

        if (apiKey != null && apiKey.equals(VALID_API_KEY)) {
            isAuthenticated = true;
        }

        if (!isAuthenticated && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (isValidJwt(token)) {
                isAuthenticated = true;
            }
        }

        if (!isAuthenticated) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Unauthorized: Invalid or missing API Key/JWT Token\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isValidJwt(String token) {
        try {
            io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            System.err.println("JWT Validation failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void destroy() {

    }

}
