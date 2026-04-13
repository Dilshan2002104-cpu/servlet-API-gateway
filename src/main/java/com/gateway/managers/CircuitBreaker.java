package com.gateway.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreaker {
    private static final int FAILURE_THRESHOLD = 5;
    private static final long RECOVERY_TIMEOUT = 30000; // 30 seconds

    private static final Map<String, AtomicInteger> failureCounts = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastFailureTime = new ConcurrentHashMap<>();

    public static boolean isAllowed(String targetUrl) {
        if (failureCounts.getOrDefault(targetUrl, new AtomicInteger(0)).get() >= FAILURE_THRESHOLD) {
            long lastFail = lastFailureTime.getOrDefault(targetUrl, 0L);
            if (System.currentTimeMillis() - lastFail < RECOVERY_TIMEOUT) {
                return false;
            } else {
                // Time to try again (Half-open)
                failureCounts.get(targetUrl).set(0);
            }
        }
        return true;
    }

    public static void recordFailure(String targetUrl) {
        failureCounts.computeIfAbsent(targetUrl, k -> new AtomicInteger(0)).incrementAndGet();
        lastFailureTime.put(targetUrl, System.currentTimeMillis());
    }

    public static void recordSuccess(String targetUrl) {
        failureCounts.getOrDefault(targetUrl, new AtomicInteger(0)).set(0);
    }
}
