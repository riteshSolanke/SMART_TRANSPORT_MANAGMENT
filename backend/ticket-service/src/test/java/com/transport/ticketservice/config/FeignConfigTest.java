package com.transport.ticketservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeignConfigTest {

    private final RequestInterceptor interceptor =
            new FeignConfig().requestInterceptor("gateway-secret");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preservesExplicitIdentityWhenCircuitBreakerThreadHasNoSecurityContext() {
        SecurityContextHolder.clearContext();
        RequestTemplate template = new RequestTemplate();
        template.header("X-User-Id", "42");
        template.header("X-User-Role", "PASSENGER");

        interceptor.apply(template);

        assertThat(template.headers().get("X-Gateway-Key"))
                .containsExactly("gateway-secret");
        assertThat(template.headers().get("X-User-Id"))
                .containsExactly("42");
        assertThat(template.headers().get("X-User-Role"))
                .containsExactly("PASSENGER");
    }

    @Test
    void addsThreadLocalIdentityWhenExplicitHeadersAreAbsent() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "77", null,
                List.of(new SimpleGrantedAuthority("ROLE_CONDUCTOR")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("X-Gateway-Key"))
                .containsExactly("gateway-secret");
        assertThat(template.headers().get("X-User-Id"))
                .containsExactly("77");
        assertThat(template.headers().get("X-User-Role"))
                .containsExactly("CONDUCTOR");
    }
}
