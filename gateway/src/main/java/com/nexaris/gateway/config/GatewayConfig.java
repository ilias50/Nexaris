package com.nexaris.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Configuration
public class GatewayConfig {

    @Value("${gateway.internal.secret}")
    private String internalSecret;

    @Bean
    public GlobalFilter customGlobalFilter() {
        return (exchange, chain) -> {
            // On ajoute le secret interne à la requête avant de la transmettre
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-Internal-Secret", internalSecret)
                    .build();

            return chain.filter(exchange.mutate().request(request).build());
        };
    }
}