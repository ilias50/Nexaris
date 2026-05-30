package com.nexaris.planningservice.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GatewayFilterTest {

    @Mock
    private FilterChain filterChain;

    @Test
    void doFilterInternal_ShouldReturn403_WhenSecretMissing() throws Exception {
        GatewayFilter filter = new GatewayFilter();
        ReflectionTestUtils.setField(filter, "internalSecret", "planning-secret");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/planning/users/1/preferences");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldPass_WhenSecretMatches() throws Exception {
        GatewayFilter filter = new GatewayFilter();
        ReflectionTestUtils.setField(filter, "internalSecret", "planning-secret");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/planning/users/1/preferences");
        request.addHeader("X-Internal-Secret", "planning-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
