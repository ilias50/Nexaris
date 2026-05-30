package com.nexaris.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final GatewayFilter gatewayFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(GatewayFilter gatewayFilter, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.gatewayFilter = gatewayFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(unauthorizedEntryPoint())
                )
                .authorizeHttpRequests(auth -> auth
                        // Déclare les exceptions AVANT toute chose
                    .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/verify-token", "/api/v1/auth/registration-enabled").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/auth/internal/user/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/auth/user/*/profile-image").permitAll()
                        .anyRequest().authenticated()
                )
                // Les filtres ne devraient s'activer que pour les routes authentifiées
                // ou gérer intelligemment le cas "permitAll"
                .addFilterBefore(gatewayFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}