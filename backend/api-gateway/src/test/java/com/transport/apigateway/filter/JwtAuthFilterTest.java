package com.transport.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    private static final String JWT_SECRET =
            "test-jwt-secret-that-is-at-least-thirty-two-bytes-long";
    private static final String GATEWAY_SECRET =
            "test-gateway-secret-that-is-at-least-thirty-two-bytes";

    private final JwtAuthFilter filter =
            new JwtAuthFilter(new ObjectMapper().findAndRegisterModules(),
                    JWT_SECRET, GATEWAY_SECRET);

    @Test
    void accessTokenReplacesClientSuppliedTrustedHeaders() {
        String token = token("ACCESS", "PASSENGER");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/routes/1")
                        .header("Authorization", "Bearer " + token)
                        .header(JwtAuthFilter.USER_ID_HEADER, "999")
                        .header(JwtAuthFilter.USER_ROLE_HEADER, "ADMIN")
                        .header(JwtAuthFilter.GATEWAY_KEY_HEADER, "attacker-value")
                        .header(JwtAuthFilter.PAYMENT_SERVICE_KEY_HEADER,
                                "attacker-payment-key")
                        .header(JwtAuthFilter.ANALYTICS_SERVICE_KEY_HEADER,
                                "attacker-analytics-key")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.apply(new JwtAuthFilter.Config())
                .filter(exchange, capture(forwarded))
                .block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders()
                .get(JwtAuthFilter.USER_ID_HEADER)).containsExactly("42");
        assertThat(forwarded.get().getRequest().getHeaders()
                .get(JwtAuthFilter.USER_ROLE_HEADER)).containsExactly("PASSENGER");
        assertThat(forwarded.get().getRequest().getHeaders()
                .get(JwtAuthFilter.GATEWAY_KEY_HEADER)).containsExactly(GATEWAY_SECRET);
        assertThat(forwarded.get().getRequest().getHeaders()
                .containsKey(JwtAuthFilter.PAYMENT_SERVICE_KEY_HEADER)).isFalse();
        assertThat(forwarded.get().getRequest().getHeaders()
                .containsKey(JwtAuthFilter.ANALYTICS_SERVICE_KEY_HEADER)).isFalse();
    }

    @Test
    void refreshTokenCannotAuthenticateAnApiRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/routes/1")
                        .header("Authorization", "Bearer " + token("REFRESH", "ADMIN"))
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.apply(new JwtAuthFilter.Config())
                .filter(exchange, capture(forwarded))
                .block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void corsPreflightDoesNotRequireJwtAndRemovesTrustedHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/tickets")
                        .header(JwtAuthFilter.USER_ROLE_HEADER, "ADMIN")
                        .header(JwtAuthFilter.PAYMENT_SERVICE_KEY_HEADER,
                                "attacker-payment-key")
                        .header(JwtAuthFilter.ANALYTICS_SERVICE_KEY_HEADER,
                                "attacker-analytics-key")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.apply(new JwtAuthFilter.Config())
                .filter(exchange, capture(forwarded))
                .block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders()
                .containsKey(JwtAuthFilter.USER_ROLE_HEADER)).isFalse();
        assertThat(forwarded.get().getRequest().getHeaders()
                .containsKey(JwtAuthFilter.PAYMENT_SERVICE_KEY_HEADER)).isFalse();
        assertThat(forwarded.get().getRequest().getHeaders()
                .containsKey(JwtAuthFilter.ANALYTICS_SERVICE_KEY_HEADER)).isFalse();
    }

    @Test
    void rejectsUnknownRoleClaim() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/routes")
                        .header("Authorization", "Bearer " + token("ACCESS", "SUPER_ADMIN"))
                        .build());

        filter.apply(new JwtAuthFilter.Config())
                .filter(exchange, ignored -> Mono.empty())
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private GatewayFilterChain capture(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private String token(String type, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject("42")
                .claim("type", type)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 60_000))
                .signWith(
                        Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }
}
