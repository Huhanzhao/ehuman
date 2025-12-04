package com.helper.commonauth.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JwtUtils {

    // 秘钥
    private static final String SECRET_KEY = "X8v!2hQ@kZ7r%pL4nB9wT6yG3sD1eF0u";
    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_MS = 30 * 60 * 1000; // 30分钟
    private static final long REFRESH_EXPIRATION_MS = 24 * 60 * 60 * 1000 * 7; // 7天

    // 生成 身份验证token
    public static String generateToken(String userId, String version) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("version", version) // 自定义字段 version 用来实现顶号功能
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    // 生成 身份刷新token
    public static String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    // 校验 JWT
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    // 解析 JWT 获取 userId
    public static String getUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        }catch (ExpiredJwtException e){
            return e.getClaims().getSubject();
        }
    }

    // 解析 JWT 获取 version
    public static String getVersion(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("version", String.class);
        }catch (ExpiredJwtException e){
            return e.getClaims().get("version", String.class);
        }
    }
}