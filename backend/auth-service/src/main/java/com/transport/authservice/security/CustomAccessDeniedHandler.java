package com.transport.authservice.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {


 private final ObjectMapper objectMapper;
 private final MessageSource messageSource;


 @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException{
     String message = messageSource.getMessage("access.denied", null, "You do not have permission to access this resource", LocaleContextHolder.getLocale());

     Map<String, Object> body = new LinkedHashMap<>();
     body.put("status", HttpServletResponse.SC_FORBIDDEN);
     body.put("message", message);
     body.put("timestamp", LocalDateTime.now().toString());

     response.setStatus(HttpServletResponse.SC_FORBIDDEN);
     response.setContentType(MediaType.APPLICATION_JSON_VALUE);
     response.getWriter().write(objectMapper.writeValueAsString(body));
 }
}
