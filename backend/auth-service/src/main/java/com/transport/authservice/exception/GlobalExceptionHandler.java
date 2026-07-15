package com.transport.authservice.exception;


import com.transport.authservice.dto.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

//    Helper..
    private ResponseEntity<ErrorResponseDto> buildErrorResponse(HttpStatus status, String message){
        ErrorResponseDto errorDto = new ErrorResponseDto(status.value(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(errorDto);
    }

//    Specific business exception
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req){

        String translatedMessage = messageSource.getMessage(
                ex.getMessage(), null, ex.getMessage(), LocaleContextHolder.getLocale()
        );

        log.warn("Resource Not Found Exception at {}: {}", req.getRequestURI(), ex.getMessage());

        return buildErrorResponse(HttpStatus.NOT_FOUND, translatedMessage);

    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req){
        String translatedMessage = messageSource.getMessage(
                ex.getMessage(), null, ex.getMessage(), LocaleContextHolder.getLocale()
        );

        log.warn("Duplicate resource found at {}: {}", req.getRequestURI(), ex.getMessage());

        return buildErrorResponse(HttpStatus.CONFLICT, translatedMessage);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req){

        String translatedMessage = messageSource.getMessage(
                ex.getMessage(), null, ex.getMessage(), LocaleContextHolder.getLocale()
        );

        log.warn("Invalid credentials at {}: {}", req.getRequestURI(), ex.getMessage());

        return buildErrorResponse(HttpStatus.UNAUTHORIZED,translatedMessage);
    }


    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidOtp( InvalidOtpException ex, HttpServletRequest req){

        String translatedMessage = messageSource.getMessage(
                ex.getMessage(), null, ex.getMessage(), LocaleContextHolder.getLocale()
        );
        log.warn("Invalid credentials at {}: {}", req.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, translatedMessage);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidToken(InvalidTokenException ex, HttpServletRequest req){

        String translatedMessage = messageSource.getMessage(
                ex.getMessage(), null, ex.getMessage(), LocaleContextHolder.getLocale()
        );

        log.warn("Invalid token found at {}: {}", req.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, translatedMessage);
    }



//    DTO validation failuers...

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest req){
        Map<String, String> fieldErrors = new HashMap<>();
        for(FieldError error: ex.getBindingResult().getFieldErrors()){
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("Status", HttpStatus.BAD_REQUEST.value());
        body.put("message" , "Validation failed");
        body.put("errors", fieldErrors);
        body.put("timestamp", LocalDateTime.now());

        log.warn("Validation failed at {}: {}", req.getRequestURI(), fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


//    Catch-all fallback

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex, HttpServletRequest req){

        log.error("Unexpected errors at {} : {}", req.getRequestURI(), ex);

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later..");
    }

}
