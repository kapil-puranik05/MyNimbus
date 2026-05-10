package com.infra.gateway.filters;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(-1)
public class AppRoutingFilter implements GlobalFilter {

    private final WebClient webClient = WebClient.builder().build();
    private final Map<String, Integer> portCache = new ConcurrentHashMap<>();
    private final Map<String, String> sessionDeploymentMap = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        System.out.println("\n=== REQUEST === Path: " + path);
        
        // Let backend routes through
        if (path.startsWith("/app/")) {
            return chain.filter(exchange);
        }
        
        // Get session ID
        String sessionId = getSessionId(exchange);
        
        // Try to get deployment ID
        String deploymentId = getDeploymentId(exchange, path, sessionId);
        if (deploymentId == null) {
            if (path.equals("/favicon.ico")) {
                exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                return exchange.getResponse().setComplete();
            }
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }
        
        // Get port
        Integer cachedPort = portCache.get(deploymentId);
        if (cachedPort != null) {
            return forward(exchange, chain, deploymentId, cachedPort);
        }
        
        // Fetch port
        String token = exchange.getRequest().getQueryParams().getFirst("token");
        final String finalDeploymentId = deploymentId;
        
        return webClient.post()
                .uri("http://localhost:8081/deploy/get-app-port")
                .header("Authorization", token != null ? "Bearer " + token : "")
                .bodyValue(Map.of("deploymentId", deploymentId))
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    int fetchedPort = (Integer) response.get("hostPort");
                    portCache.put(finalDeploymentId, fetchedPort);
                    System.out.println("✅ Port for " + finalDeploymentId + " is " + fetchedPort);
                    return forward(exchange, chain, finalDeploymentId, fetchedPort);
                })
                .onErrorResume(error -> {
                    System.err.println("❌ Error: " + error.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                    return exchange.getResponse().setComplete();
                });
    }
    
    private String getDeploymentId(ServerWebExchange exchange, String path, String sessionId) {
        // From path
        String[] parts = path.split("/");
        if (parts.length > 1 && parts[1].length() >= 36) {
            sessionDeploymentMap.put(sessionId, parts[1]);
            System.out.println("✅ Deployment from path: " + parts[1]);
            return parts[1];
        }
        
        // From session
        String deploymentId = sessionDeploymentMap.get(sessionId);
        if (deploymentId != null) {
            System.out.println("✅ Deployment from session: " + deploymentId);
            return deploymentId;
        }
        
        // From referer
        String referer = exchange.getRequest().getHeaders().getFirst("Referer");
        if (referer != null && referer.contains("/")) {
            String[] refParts = referer.split("/");
            for (String part : refParts) {
                if (part.length() >= 36) {
                    sessionDeploymentMap.put(sessionId, part);
                    System.out.println("✅ Deployment from referer: " + part);
                    return part;
                }
            }
        }
        
        return null;
    }
    
    private String getSessionId(ServerWebExchange exchange) {
        var cookie = exchange.getRequest().getCookies().getFirst("SESSION");
        if (cookie != null) {
            return cookie.getValue();
        }
        return java.util.UUID.randomUUID().toString();
    }
    
    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilterChain chain, 
                                String deploymentId, int targetPort) {
        String originalPath = exchange.getRequest().getURI().getPath();
        String query = exchange.getRequest().getURI().getQuery();
        
        // Remove deployment ID from path
        String cleanPath = originalPath.replaceFirst("/" + deploymentId, "");
        if (cleanPath.isEmpty()) {
            cleanPath = "/";
        }
        
        // Build target URL
        String targetUrl = "http://localhost:" + targetPort + cleanPath;
        if (query != null && !query.isEmpty()) {
            targetUrl += "?" + query;
        }
        
        System.out.println("🎯 Forwarding: " + originalPath + " -> " + targetUrl);
        
        // Forward request
        URI targetUri = URI.create(targetUrl);
        ServerWebExchange mutated = exchange.mutate()
                .request(builder -> builder.uri(targetUri))
                .build();
        
        mutated.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, targetUri);
        mutated.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ALREADY_ROUTED_ATTR, true);
        
        return chain.filter(mutated);
    }
}