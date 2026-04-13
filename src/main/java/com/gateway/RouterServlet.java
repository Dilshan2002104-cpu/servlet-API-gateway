package com.gateway;

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

        try {
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(targetUrl))
                    .method(request.getMethod(), getRequestBody(request));

            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!isRestrictedHeader(headerName)) {
                    requestBuilder.header(headerName, request.getHeader(headerName));
                }
            }

            java.net.http.HttpResponse<byte[]> proxyResponse = httpClient.send(requestBuilder.build(), 
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray());

            response.setStatus(proxyResponse.statusCode());
            proxyResponse.headers().map().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("Transfer-Encoding") && !name.equalsIgnoreCase("Content-Length")) {
                    values.forEach(value -> response.addHeader(name, value));
                }
            });

            response.getOutputStream().write(proxyResponse.body());
            
        } catch (Exception e) {
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
