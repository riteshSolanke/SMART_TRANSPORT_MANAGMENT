package com.transport.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secret=test-jwt-secret-that-is-at-least-thirty-two-bytes",
                "gateway.secret-key=test-gateway-secret-that-is-at-least-thirty-two-bytes",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false"
        })
class GatewayCorsIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void configuredFrontendOriginCanCompletePreflightWithoutJwt() {
        WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build()
                .method(HttpMethod.OPTIONS)
                .uri("/api/routes")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173");
    }
}
