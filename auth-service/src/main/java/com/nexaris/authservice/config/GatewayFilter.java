package com.nexaris.authservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class GatewayFilter extends OncePerRequestFilter {
    @Value("${gateway.internal.secret}")
    private String internalSecret;

    @Value("${service.token:}")
    private String serviceToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        
        // Routes internes (inter-services) - validées avec X-Service-Token
        if (requestPath.contains("/internal/")) {
            String incomingServiceToken = request.getHeader("X-Service-Token");
            if (serviceToken == null || serviceToken.isBlank() || !serviceToken.equals(incomingServiceToken)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Service token invalide ou absent.");
                return;
            }
        } 
        // Routes gateway - validées avec X-Internal-Secret
        else {
            String incomingSecret = request.getHeader("X-Internal-Secret");
            if (internalSecret == null || !internalSecret.equals(incomingSecret)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Accès direct non autorisé. Passez par la Gateway.");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}