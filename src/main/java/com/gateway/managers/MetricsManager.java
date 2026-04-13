package com.gateway.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsManager {
    private static final AtomicLong totalRequests = new AtomicLong(0);
    private static final AtomicLong failedRequests = new AtomicLong(0);
    private static final Map<String, AtomicLong> routeHits = new ConcurrentHashMap<>();

    public static void incrementTotalRequests() {
        totalRequests.incrementAndGet();
    }

    public static void incrementFailedRequests() {
        failedRequests.incrementAndGet();
    }

    public static void incrementRouteHit(String path) {
        routeHits.computeIfAbsent(path, k -> new AtomicLong(0)).incrementAndGet();
    }

    public static Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("totalRequests", totalRequests.get());
        metrics.put("failedRequests", failedRequests.get());
        metrics.put("routeHits", routeHits);
        return metrics;
    }
}
