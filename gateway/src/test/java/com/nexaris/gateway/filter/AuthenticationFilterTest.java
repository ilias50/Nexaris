package com.nexaris.gateway.filter;

import com.nexaris.gateway.dto.AuthResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Test
    void apply_ShouldReturn401_WhenAuthorizationHeaderMissing() {
        AuthenticationFilter filterFactory = new AuthenticationFilter(webClientBuilder);
        ReflectionTestUtils.setField(filterFactory, "internalSecret", "internal-secret");
                ReflectionTestUtils.setField(filterFactory, "authVerifyTokenUri", "http://auth-service:8080/api/v1/auth/verify-token");

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/org/tree").build()
        );
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filterFactory.apply(new AuthenticationFilter.Config()).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void apply_ShouldForwardRequestWithInternalHeaders_WhenTokenIsValid() {
        AuthenticationFilter filterFactory = new AuthenticationFilter(webClientBuilder);
        ReflectionTestUtils.setField(filterFactory, "internalSecret", "internal-secret");
                ReflectionTestUtils.setField(filterFactory, "authVerifyTokenUri", "http://auth-service:8080/api/v1/auth/verify-token");

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestSpec = mock(WebClient.RequestBodyUriSpec.class, Answers.RETURNS_SELF);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AuthResponse.class))
                .thenReturn(Mono.just(new AuthResponse(true, 42, "ok")));

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/org/tree")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .build()
        );

        filterFactory.apply(new AuthenticationFilter.Config()).filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(exchangeCaptor.capture());

        ServerHttpRequest forwardedRequest = exchangeCaptor.getValue().getRequest();
        assertThat(forwardedRequest.getHeaders().getFirst("X-Internal-Secret")).isEqualTo("internal-secret");
        assertThat(forwardedRequest.getHeaders().getFirst("X-User-Id")).isEqualTo("42");
        verify(requestSpec).uri(eq("http://auth-service:8080/api/v1/auth/verify-token"));
        verify(requestSpec).header(eq(HttpHeaders.AUTHORIZATION), anyString());
                verify(requestSpec).header(eq("X-Internal-Secret"), eq("internal-secret"));
    }

    @Test
    void apply_ShouldReturn401_WhenAuthServiceRejectsToken() {
        AuthenticationFilter filterFactory = new AuthenticationFilter(webClientBuilder);
        ReflectionTestUtils.setField(filterFactory, "internalSecret", "internal-secret");
                ReflectionTestUtils.setField(filterFactory, "authVerifyTokenUri", "http://auth-service:8080/api/v1/auth/verify-token");

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestSpec = mock(WebClient.RequestBodyUriSpec.class, Answers.RETURNS_SELF);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AuthResponse.class))
                .thenReturn(Mono.just(new AuthResponse(false, null, "invalid")));

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/org/tree")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .build()
        );

        filterFactory.apply(new AuthenticationFilter.Config()).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }
}
