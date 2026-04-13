package com.infra.gateway.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ExternalConfiguration {
    @Value("${mynimbus.url}")
    public String mynimbusUrl;

    @Bean("mynimbus-client")
    public WebClient webClient() {
        return WebClient.create(mynimbusUrl);
    }
}
