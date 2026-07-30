package com.transport.apigateway.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.transport.apigateway.dto.ErrorResponseDto;


@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config>{

    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLE_HEADER = "X-User-Role";
    static final String GATEWAY_KEY_HEADER = "X-Gateway-Key";
    static final String PAYMENT_SERVICE_KEY_HEADER = "X-Payment-Service-Key";
    static final String ANALYTICS_SERVICE_KEY_HEADER = "X-Analytics-Service-Key";
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "PASSENGER", "CONDUCTOR", "DISPATCHER", "TRANSPORT_MANAGER", "ADMIN");

    private final String secret;
    private final String gatewaySecret;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(
            ObjectMapper objectMapper,
            @Value("${jwt.secret}") String secret,
            @Value("${gateway.secret-key}") String gatewaySecret) {
        super(Config.class);
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        if (gatewaySecret == null || gatewaySecret.length() < 32) {
            throw new IllegalStateException("GATEWAY_SHARED_SECRET must contain at least 32 characters");
        }
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.gatewaySecret = gatewaySecret;
    }

    @Override
    public GatewayFilter apply(Config config){
        return (exchange, chain) ->{
            String path = exchange.getRequest().getURI().getPath();

            if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
                return chain.filter(removeTrustedHeaders(exchange));
            }

            List<String> authHeaders = exchange.getRequest().getHeaders().get("Authorization");

            if (authHeaders == null || authHeaders.size() != 1) {
                log.warn("Request to {} rejected - missing or ambiguous Authorization header", path);
                return onError(exchange,  "Missing authentication token", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = authHeaders.get(0);
            if (!authHeader.startsWith("Bearer ") || authHeader.length() == 7) {
                return onError(exchange, "Invalid authentication scheme", HttpStatus.UNAUTHORIZED);
            }
            String token = authHeader.substring(7).trim();

            try{
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String userId = claims.getSubject();
                String role = claims.get("role", String.class);
                String tokenType = claims.get("type", String.class);

                if (!"ACCESS".equals(tokenType)) {
                    return onError(exchange, "Only access tokens can be used for API requests",
                            HttpStatus.UNAUTHORIZED);
                }
                if (userId == null || userId.isBlank() || role == null || !ALLOWED_ROLES.contains(role)) {
                    return onError(exchange, "Token is missing required identity claims",
                            HttpStatus.UNAUTHORIZED);
                }

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.headers(headers -> {
                            headers.remove(USER_ID_HEADER);
                            headers.remove(USER_ROLE_HEADER);
                            headers.remove(GATEWAY_KEY_HEADER);
                            headers.remove(PAYMENT_SERVICE_KEY_HEADER);
                            headers.remove(ANALYTICS_SERVICE_KEY_HEADER);
                            headers.set(USER_ID_HEADER, userId);
                            headers.set(USER_ROLE_HEADER, role);
                            headers.set(GATEWAY_KEY_HEADER, gatewaySecret);
                        })).build();

                return chain.filter(mutatedExchange);
            }
            catch (ExpiredJwtException e) {

                log.warn("JWT Token Expired");

                return onError(
                        exchange,
                        "Access token has expired. Please login again.",
                        HttpStatus.UNAUTHORIZED);
            }

            catch (JwtException | IllegalArgumentException e) {
                log.warn("JWT validation failed for path {}: {}", path, e.getClass().getSimpleName());
                return onError(exchange, "Invalid authentication token", HttpStatus.UNAUTHORIZED);
            }

            catch (Exception e) {
                log.error("Unexpected authentication failure for path {}", path, e);
                return onError(exchange, "Authentication failed", HttpStatus.UNAUTHORIZED);
            }

        };
    }

    private ServerWebExchange removeTrustedHeaders(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                    headers.remove(GATEWAY_KEY_HEADER);
                    headers.remove(PAYMENT_SERVICE_KEY_HEADER);
                    headers.remove(ANALYTICS_SERVICE_KEY_HEADER);
                }))
                .build();
    }

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatusCode status){
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponseDto errorDto = new ErrorResponseDto(status.value(), message, LocalDateTime.now());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorDto);
            DataBuffer buffer =
                    exchange.getResponse()
                            .bufferFactory()
                            .wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Serialization Error", e);
            return exchange.getResponse().setComplete();
        }
    }


    public static class Config{

    }


}
