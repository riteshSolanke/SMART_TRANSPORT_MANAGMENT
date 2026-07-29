package com.transport.routeservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InternalValidationFilterTest {

    private static final String GATEWAY_SECRET =
            "test-gateway-secret-that-is-at-least-thirty-two-bytes";
    private final InternalValidationFilter filter =
            new InternalValidationFilter(GATEWAY_SECRET);

    @Test
    void rejectsDirectBusinessRequestWithoutGatewaySecret() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/routes/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void acceptsRequestWithConfiguredGatewaySecret() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/routes/1");
        request.addHeader("X-Gateway-Key", GATEWAY_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void healthEndpointDoesNotRequireInternalSecret() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }
}
