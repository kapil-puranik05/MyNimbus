package com.infra.gateway.filters;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(-100)
public class AppRoutingFilter implements GlobalFilter {

    private final WebClient webClient = WebClient.create("http://localhost:8081");
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        System.out.println("FILTER HIT: " + path);

        if (path.startsWith("/app/")) {
            return chain.filter(exchange);
        }

        String[] parts = path.split("/");
        if (parts.length < 2) {
            return chain.filter(exchange);
        }

        String deploymentIdStr = parts[1];

        try {
            UUID.fromString(deploymentIdStr);
        } catch (IllegalArgumentException e) {
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getQueryParams().getFirst("token");
        System.out.println("TOKEN: " + token);

        Integer cachedPort = cache.get(deploymentIdStr);
        if (cachedPort != null) {
            System.out.println("Cache hit → " + cachedPort);
            return forward(exchange, chain, path, deploymentIdStr, cachedPort);
        }

        WebClient.RequestBodySpec request = webClient.post()
                .uri("/deploy/get-app-port");

        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        return request
                .bodyValue(new PortRequest(deploymentIdStr))
                .retrieve()
                .bodyToMono(PortResponse.class)
                .doOnNext(res -> System.out.println("Resolved port: " + res.getHostPort()))
                .flatMap(response -> {
                    int hostPort = response.getHostPort();
                    cache.put(deploymentIdStr, hostPort);
                    return forward(exchange, chain, path, deploymentIdStr, hostPort);
                })
                .doOnError(err -> System.out.println("Backend error: " + err.getMessage()))
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                    return exchange.getResponse().setComplete();
                });
    }

    private Mono<Void> forward(ServerWebExchange exchange,
                              GatewayFilterChain chain,
                              String path,
                              String deploymentId,
                              int hostPort) {

        String newPath = path.replaceFirst("/" + deploymentId, "");
        if (newPath.isEmpty()) {
            newPath = "/";
        }

        String target = "http://localhost:" + hostPort + newPath;
        System.out.println("Forwarding to: " + target);

        URI uri = URI.create(target);

        ServerWebExchange newExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .uri(uri)
                        .build())
                .build();

        return chain.filter(newExchange);
    }

    static class PortRequest {
        private String deploymentId;

        public PortRequest() {}

        public PortRequest(String deploymentId) {
            this.deploymentId = deploymentId;
        }

        public String getDeploymentId() {
            return deploymentId;
        }

        public void setDeploymentId(String deploymentId) {
            this.deploymentId = deploymentId;
        }
    }

    static class PortResponse {
        private int hostPort;

        public int getHostPort() {
            return hostPort;
        }

        public void setHostPort(int hostPort) {
            this.hostPort = hostPort;
        }
    }
}