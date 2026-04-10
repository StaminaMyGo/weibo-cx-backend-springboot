package com.hn.it.weibo.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    // 密钥（随便写的一串长字符串，实际项目中建议放在配置文件里）
    private static final String SECRET_KEY = "MySuperSecretKeyForJwtTokenGenerationWhichMustBeLongEnough";

    // 1. 生成 Token 的方法
    public String createToken(String username, Long userId) {
        long expirationTime = 1000 * 60 * 60 * 24; // 24小时过期
        Date expireDate = new Date(System.currentTimeMillis() + expirationTime);

        return Jwts.builder()
                .subject(username)           // 设置用户名
                .claim("userId", userId)     // 存入用户ID
                .issuedAt(new Date())        // 签发时间
                .expiration(expireDate)      // 过期时间
                .signWith(getSigningKey())   // 签名
                .compact();
    }

    // 2. 解析 Token 的方法
    public Claims parseToken(String token) {
        SecretKey key = getSigningKey();
        return Jwts.parser()
                .verifyWith(key)             // 设置密钥
                .build()                     // 0.12.5 必须先 build
                .parseSignedClaims(token)    // 解析
                .getPayload();               // 获取内容
    }

    // 3. 获取用户名
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    // 4. 获取密钥对象
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
}