package com.gateway.servlets;

import com.gateway.config.RouteConfig;
import com.gateway.managers.CacheManager;
import com.gateway.managers.CircuitBreaker;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/api/*")
public class RouterServlet extends HttpServlet{

    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        String targetUrl = RouteConfig.getTargetUrl(pathInfo);

        if (targetUrl == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"No route found for " + pathInfo + "\"}");
            return;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            byte[] cachedData = CacheManager.get(targetUrl);
            if (cachedData != null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.addHeader("X-Cache", "HIT");
                response.getOutputStream().write(cachedData);
                return;
            }
        }

        try {
            if (!CircuitBreaker.isAllowed(targetUrl)) {
                response.setStatus(503); // Service Unavailable
                response.getWriter().write("{\"error\": \"Service temporarily unavailable due to frequent failures (Circuit Breaker)\"}");
                return;
            }

            // Build the proxy request
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(targetUrl))
                    .header("X-Proxied-By", "Java-API-Gateway")
                    .method(request.getMethod(), getRequestBody(request));

            // Forward headers (except some restricted ones)
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!isRestrictedHeader(headerName)) {
                    requestBuilder.header(headerName, request.getHeader(headerName));
                }
            }

            java.net.http.HttpResponse<byte[]> proxyResponse = httpClient.send(requestBuilder.build(), 
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray());

            if (proxyResponse.statusCode() >= 500) {
                CircuitBreaker.recordFailure(targetUrl);
            } else {
                CircuitBreaker.recordSuccess(targetUrl);
            }

            response.setStatus(proxyResponse.statusCode());
            proxyResponse.headers().map().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("Transfer-Encoding") && !name.equalsIgnoreCase("Content-Length")) {
                    values.forEach(value -> response.addHeader(name, value));
                }
            });

            if (proxyResponse.statusCode() == 200 && "GET".equalsIgnoreCase(request.getMethod())) {
                CacheManager.put(targetUrl, proxyResponse.body());
            }

            response.getOutputStream().write(proxyResponse.body());
            
        } catch (Exception e) {
            CircuitBreaker.recordFailure(targetUrl);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Proxy error: " + e.getMessage() + "\"}");
        }
    }

    private java.net.http.HttpRequest.BodyPublisher getRequestBody(HttpServletRequest request) throws IOException {
        if ("GET".equalsIgnoreCase(request.getMethod()) || "DELETE".equalsIgnoreCase(request.getMethod())) {
            return java.net.http.HttpRequest.BodyPublishers.noBody();
        }
        return java.net.http.HttpRequest.BodyPublishers.ofByteArray(request.getInputStream().readAllBytes());
    }

    private boolean isRestrictedHeader(String headerName) {
        return headerName.equalsIgnoreCase("host") || 
               headerName.equalsIgnoreCase("content-length") ||
               headerName.equalsIgnoreCase("connection") ||
               headerName.equalsIgnoreCase("upgrade");
    }
}
