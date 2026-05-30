package com.nexaris.notificationservice.config;

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
    void doFilterInternal_ShouldReturn403_WhenSecretIsMissing() throws Exception {
        GatewayFilter filter = new GatewayFilter();
        ReflectionTestUtils.setField(filter, "internalSecret", "my-secret");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications/channels");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldContinue_WhenSecretIsValid() throws Exception {
        GatewayFilter filter = new GatewayFilter();
        ReflectionTestUtils.setField(filter, "internalSecret", "my-secret");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications/channels");
        request.addHeader("X-Internal-Secret", "my-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldBypassActuatorPath() throws Exception {
        GatewayFilter filter = new GatewayFilter();
        ReflectionTestUtils.setField(filter, "internalSecret", "my-secret");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
