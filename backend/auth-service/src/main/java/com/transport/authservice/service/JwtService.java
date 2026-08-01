package com.transport.authservice.service;


import com.transport.authservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final String secret;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        if (accessTokenExpiry <= 0 || refreshTokenExpiry <= accessTokenExpiry) {
            throw new IllegalStateException(
                    "JWT expiry values must be positive and refresh expiry must exceed access expiry");
        }
        this.secret = secret;
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }


    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }



    private String buildToken(User user, long expiryMillis, String tokenType){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMillis);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(String.valueOf(user.getUserId()))
                .claim("mobile", user.getMobileNumber())
                .claim("role", user.getRole().name())
                .claim("lang", user.getPreferredLanguage())
                .claim("type", tokenType)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }




//    Generate AccessToken
    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenExpiry, "ACCESS");
    }


//    Generate Refresh-token
    public String generateRefreshtoken(User user) {
        return buildToken(user, refreshTokenExpiry, "REFRESH");
    }


//    Short-lived token used only for OTP-verify and complete the registration steps.

    public String generateRegistrationToken(String mobileNumber) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (1000 * 60 * 10));

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(mobileNumber)
                .claim("type", "REGISTRATION")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }



//    Shor lived token used only between OTP-verify and reset-password steps

    public String generateResetToken(String mobileNumber){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (1000 * 60 * 10));

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(mobileNumber)
                .claim("type", "RESET_PASSWORD")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }



    public Claims extractClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token){
        try{
            extractClaims(token);
            return true;
        }catch(Exception e){
            log.warn("Invalid or expired token: {}", e.getClass().getSimpleName());
            return false;
        }
    }

}
