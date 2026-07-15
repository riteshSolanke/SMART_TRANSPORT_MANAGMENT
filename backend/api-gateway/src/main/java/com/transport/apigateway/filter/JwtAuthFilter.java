package com.transport.apigateway.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import org.apache.logging.slf4j.SLF4JLogBuilder;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config>{


    @Value("${jwt.secret}")
    private String secret;

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

            String token = authHeaders.get(0).replace("Bearer", "");

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
                            headers.add("X-User_ID", userId);
                            headers.add("X-User-Role", role);
                        })).build();

                return chain.filter(mutatedExchange);
            }
            catch(Exception e){
                log.error("Token validation faile for path {}:{}", path, e.getMessage());

                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

        };
    }

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }



    private reactor.core.publisher.Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status){

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        ErrorResponseDto errorDto = new ErrorResponseDto(status.value(), message, LocalDateTime.now());

        try{
            byte[] bytes = objectMapper.writeValueAsBytes(errorDto);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(reactor.core.publisher.Mono.just(buffer));
        }catch(Exception e){
            return exchange.getResponse().setComplete();
        }


    }









    public static class Config{

    }


}