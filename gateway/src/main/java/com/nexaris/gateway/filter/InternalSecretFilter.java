package com.nexaris.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class InternalSecretFilter extends AbstractGatewayFilterFactory<InternalSecretFilter.Config> {

    @Value("${gateway.internal.secret}")
    private String internalSecret;

    public InternalSecretFilter() {
        super(Config.class);
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Internal-Secret", internalSecret)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }
}
