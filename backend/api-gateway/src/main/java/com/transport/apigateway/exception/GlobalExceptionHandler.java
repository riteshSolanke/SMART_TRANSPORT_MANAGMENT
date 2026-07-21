package com.transport.apigateway.exception;


import com.transport.apigateway.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

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

        Throwable ex = getError(request);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Something went wrong";

        if (ex instanceof ConnectException ||
                (ex.getMessage() != null
                        && ex.getMessage().contains("Connection timed out"))) {

            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Target service is unavailable";
        }

        log.error("Gateway Exception", ex);

        ErrorResponseDto response = ErrorResponseDto.builder()
                .status(status.value())
                .message(message)
                .build();

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }
}