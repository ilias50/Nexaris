package com.nexaris.authservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Test
    void contextLoads() {
        // Test basique pour vérifier que la classe peut être instanciée
        JwtTokenProvider provider = new JwtTokenProvider();
        assertThat(provider).isNotNull();
    }

    @Test
    void getExpirationTimeInSeconds_ShouldReturnPositiveValue() {
        // Given
        JwtTokenProvider provider = new JwtTokenProvider();
        // Injecter les valeurs via ReflectionTestUtils pour éviter le contexte Spring
        ReflectionTestUtils.setField(provider, "jwtExpirationInMs", 3600000L);

        // When
        long expirationTime = provider.getExpirationTimeInSeconds();

        // Then
        assertThat(expirationTime).isGreaterThan(0);
        assertThat(expirationTime).isEqualTo(3600); // 3600000 ms / 1000 = 3600 seconds
    }
}