package com.nexaris.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * Filter qui valide X-Service-Token pour les appels internes,
 * puis relaie la valeur configurée au service cible.
 * 
 * Exemple d'utilisation en application.yml :
 *   - id: auth_internal_route
 *     uri: http://auth-service:8080
 *     predicates:
 *       - Path=/api/v1/auth/internal/**
 *     filters:
 *       - ServiceTokenFilter
 */
@Component
public class ServiceTokenFilter extends AbstractGatewayFilterFactory<ServiceTokenFilter.Config> {

    @Value("${service.token}")
    private String serviceToken;

    public ServiceTokenFilter() {
        super(Config.class);
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String incomingToken = exchange.getRequest().getHeaders().getFirst("X-Service-Token");
            if (serviceToken == null || serviceToken.isBlank() || !serviceToken.equals(incomingToken)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Service-Token", serviceToken)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }
}
