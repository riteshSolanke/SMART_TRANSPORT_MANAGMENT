package com.transport.vehicleservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalValidationFilterTest {
    private static final String SECRET =
            "test-gateway-secret-that-is-at-least-thirty-two-bytes";

    @Test
    void rejectsShortConfigurationSecret() {
        assertThatThrownBy(() -> new InternalValidationFilter("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsRequestWithoutGatewaySecret() throws Exception {
        InternalValidationFilter filter = new InternalValidationFilter(SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(
                "GET", "/api/vehicles/1"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void permitsHealthWithoutGatewaySecret() throws Exception {
        InternalValidationFilter filter = new InternalValidationFilter(SECRET);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/actuator/health");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }
}
