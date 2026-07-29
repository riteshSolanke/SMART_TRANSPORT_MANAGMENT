package com.transport.apigateway.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import org.springframework.core.io.buffer.DataBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.List;
import com.transport.apigateway.dto.ErrorResponseDto;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;


@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config>{

    @Value("${jwt.secret}")
    private String secret;

    @Value("${gateway.secret-key}")
    private String gatewaySecret;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public JwtAuthFilter(){
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config){
        return (exchange, chain) ->{
            String path = exchange.getRequest().getURI().getPath();

            List<String> authHeaders = exchange.getRequest().getHeaders().get("Authorization");

            if(authHeaders == null || authHeaders.isEmpty()){
                log.warn("Request to {} rejcted - missing Authorization header", path);

                return onError(exchange,  "Missing authentication token", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeaders.get(0).replace("Bearer ", "").trim();

            try{
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                log.info("Authenticated request to {} - userId={}, role={}", path, userId, role);

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.headers(headers -> {
                            headers.add("X-User-Id", userId);
                            headers.add("X-User-Role", role);
                            headers.add("X-Gateway-Key", gatewaySecret);
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

            catch (SignatureException e) {

                log.warn("Invalid JWT Signature");

                return onError(
                        exchange,
                        "Invalid token signature.",
                        HttpStatus.UNAUTHORIZED);
            }

            catch (MalformedJwtException e) {

                log.warn("Malformed JWT");

                return onError(
                        exchange,
                        "Malformed token.",
                        HttpStatus.UNAUTHORIZED);
            }

            catch (UnsupportedJwtException e) {

                log.warn("Unsupported JWT");

                return onError(
                        exchange,
                        "Unsupported token.",
                        HttpStatus.UNAUTHORIZED);
            }

            catch (Exception e) {

                log.warn("JWT Validation Failed");

                return onError(
                        exchange,
                        "Authentication failed.",
                        HttpStatus.UNAUTHORIZED);
            }

        };
    }


    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    private reactor.core.publisher.Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status){

        log.info("Inside onError()");
        log.info("Message: {}", message);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        ErrorResponseDto errorDto = new ErrorResponseDto(status.value(), message, LocalDateTime.now());

        try {

            log.info("Step 1");

            byte[] bytes = objectMapper.writeValueAsBytes(errorDto);

            log.info("Step 2");

            DataBuffer buffer =
                    exchange.getResponse()
                            .bufferFactory()
                            .wrap(bytes);

            log.info("Step 3");

            return exchange.getResponse()
                    .writeWith(Mono.just(buffer))
                    .doOnSuccess(v -> log.info("Step 4 Success"))
                    .doOnError(err -> log.error("Step 4 Error", err));

        } catch (Exception e) {

            log.error("Serialization Error", e);

            return exchange.getResponse().setComplete();
        }
    }


    public static class Config{

    }


}