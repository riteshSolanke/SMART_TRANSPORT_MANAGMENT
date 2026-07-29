package com.transport.apigateway.exception;


import com.transport.apigateway.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Order(-2)
@Slf4j
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalExceptionHandler(
            ErrorAttributes errorAttributes,
            ApplicationContext applicationContext,
            ServerCodecConfigurer codecConfigurer) {

        super(errorAttributes,
                new WebProperties.Resources(),
                applicationContext);

        this.setMessageReaders(codecConfigurer.getReaders());
        this.setMessageWriters(codecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(
            ErrorAttributes errorAttributes) {

        return RouterFunctions.route(
                RequestPredicates.all(),
                this::handleError);
    }

    private Mono<ServerResponse> handleError(ServerRequest request) {

        Throwable exception = getError(request);
        Map<String, Object> attributes =
                getErrorAttributes(request, ErrorAttributeOptions.defaults());
        int statusCode = (int) attributes.getOrDefault(
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = switch (status) {
            case NOT_FOUND -> "Requested endpoint was not found";
            case SERVICE_UNAVAILABLE, BAD_GATEWAY, GATEWAY_TIMEOUT ->
                    "Target service is unavailable";
            default -> status.is4xxClientError()
                    ? "Request could not be processed"
                    : "Internal server error";
        };

        if (status.is5xxServerError()) {
            log.error("Gateway request failed with status {}", status.value(), exception);
        } else {
            log.warn("Gateway request failed with status {}: {}",
                    status.value(), exception.getClass().getSimpleName());
        }

        ErrorResponseDto response = ErrorResponseDto.builder()
                .status(status.value())
                .message(message)
                .build();

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }
}
