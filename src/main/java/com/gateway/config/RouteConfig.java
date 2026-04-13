package com.gateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RouteConfig {
    private static final Map<String, List<String>> routes = new HashMap<>();
    private static final Map<String, AtomicInteger> roundRobinCounters = new HashMap<>();

    static {
        reloadRoutes();
    }

    public static void reloadRoutes() {
        routes.clear();
        roundRobinCounters.clear();
        try (InputStream is = RouteConfig.class.getClassLoader().getResourceAsStream("routes.json")) {
            if (is != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(is);
                JsonNode routesNode = root.get("routes");
                if (routesNode.isArray()) {
                    for (JsonNode node : routesNode) {
                        String path = node.get("path").asText();
                        List<String> targetList = new ArrayList<>();
                        
                        if (node.has("targets")) {
                            for (JsonNode t : node.get("targets")) {
                                targetList.add(t.asText());
                            }
                        } else if (node.has("target")) {
                            targetList.add(node.get("target").asText());
                        }
                        
                        routes.put(path, targetList);
                        roundRobinCounters.put(path, new AtomicInteger(0));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading routes: " + e.getMessage());
        }
    }

    public static String getTargetUrl(String path) {
        if (path == null) return null;
        for (Map.Entry<String, List<String>> entry : routes.entrySet()) {
            if (path.contains(entry.getKey())) {
                List<String> targets = entry.getValue();
                if (targets.isEmpty()) return null;
                
                int index = roundRobinCounters.get(entry.getKey()).getAndIncrement() % targets.size();
                return targets.get(Math.abs(index));
            }
        }
        return null;
    }
}
