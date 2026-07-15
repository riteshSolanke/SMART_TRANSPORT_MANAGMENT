package com.transport.authservice.security;

import com.transport.authservice.config.JwtLocaleResolver;
import com.transport.authservice.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain filterChain ) throws ServletException, IOException {

        String authHeader = req.getHeader("Authorization");

//        No token present - let it pass through; SecurityConfig decides if route needs auth
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(req, res);
            return;
        }

        String token = authHeader.substring(7);

        try{
            Claims claims = jwtService.extractClaims(token);
            String tokenType = claims.get("type", String.class);

            String lang = claims.get("lang", String.class);

            System.out.println("Language set: " + lang);
            if(lang != null){
                req.setAttribute(JwtLocaleResolver.LOCALE_ATTRIBUTE , new Locale(lang)
                );
            }

//            Only Access tokens are valid for general api authentication...
            if(!"ACCESS".equals(tokenType)){
                filterChain.doFilter(req, res);
                return;
            }

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

//            Forward userid downstream - Controllers read this via  @RequestHeader

            req.setAttribute("X-User-Id", userId);

        }catch(Exception e){
            log.warn("Invalid JWT token: {}", e.getMessage());
//          Don't throw exception here ... security config will reject it
//            Unauthenticated access to protected routes with 401 automatically
        }

        filterChain.doFilter(req,res);
    }

}

