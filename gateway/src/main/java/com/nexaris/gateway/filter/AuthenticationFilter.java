package com.nexaris.gateway.filter;

import com.nexaris.gateway.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final WebClient.Builder webClientBuilder;

    @Value("${gateway.internal.secret}")
    private String internalSecret;

    @Value("${gateway.auth.verify-token-uri}")
    private String authVerifyTokenUri;

    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // On récupère les headers de manière réactive
            HttpHeaders headers = exchange.getRequest().getHeaders();

            // 1. Vérification alternative à containsKey
            List<String> authHeaderList = headers.get(HttpHeaders.AUTHORIZATION);

            if (authHeaderList == null || authHeaderList.isEmpty()) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String authHeader = authHeaderList.get(0);

            // 2. Appel à l'Auth-Service
            return webClientBuilder.build()
                    .post()
                    .uri(authVerifyTokenUri)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .bodyToMono(AuthResponse.class)
                    .flatMap(authResponse -> {
                        if (authResponse != null && authResponse.isValid()) {
                            String rolesHeader = authResponse.getRoles() == null
                                ? ""
                                : authResponse.getRoles().stream().collect(Collectors.joining(","));
                            // 3. Mutation de la requête (ajout des headers internes)
                            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                    .header("X-Internal-Secret", internalSecret)
                                    .header("X-User-Id", String.valueOf(authResponse.getUserId()))
                                .header("X-User-Roles", rolesHeader)
                                    .build();

                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        }

                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    })
                    .onErrorResume(e -> {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }
}