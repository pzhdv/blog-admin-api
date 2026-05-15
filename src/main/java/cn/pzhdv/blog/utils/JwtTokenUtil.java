package cn.pzhdv.blog.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 工具类
 * 功能：统一处理 Token 生成、解析、验证、提取用户信息
 * 支持：访问令牌(AccessToken) + 刷新令牌(RefreshToken)
 *
 * @author PanZonghui
 */
@Slf4j
@Component
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtTokenUtil {

    /**
     * 请求头名称（如：Authorization）
     */
    private String header;

    /**
     * JWT 签名密钥（配置文件注入）
     */
    private String tokenSignKey;

    /**
     * 访问令牌有效期（单位：秒）
     */
    private Integer accessTokenExpire;

    /**
     * 刷新令牌有效期（单位：秒）
     */
    private Integer refreshTokenExpire;

    // ============================== 对外接口 ==============================

    /**
     * 生成访问令牌（短有效期，用于接口鉴权）
     * @param userId 用户ID
     * @return JWT 访问令牌
     */
    public String generateAccessToken(String userId) {
        return generateToken(userId, accessTokenExpire);
    }

    /**
     * 生成访问令牌（带用户名，避免每次请求查库）
     */
    public String generateAccessToken(String userId, String username) {
        return generateToken(userId, username, accessTokenExpire);
    }

    /**
     * 生成刷新令牌（长有效期，用于获取新的访问令牌）
     * @param userId 用户ID
     * @return JWT 刷新令牌
     */
    public String generateRefreshToken(String userId) {
        return generateToken(userId, refreshTokenExpire);
    }

    /**
     * 生成刷新令牌（带用户名）
     */
    public String generateRefreshToken(String userId, String username) {
        return generateToken(userId, username, refreshTokenExpire);
    }

    /**
     * 验证访问令牌是否有效
     * @param token 访问令牌
     * @return true=有效 false=无效
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token);
    }

    /**
     * 验证刷新令牌是否有效
     * @param refreshToken 刷新令牌
     * @return true=有效 false=无效
     */
    public boolean validateRefreshToken(String refreshToken) {
        return validateToken(refreshToken);
    }

    /**
     * 从访问令牌解析用户ID
     * @param token 访问令牌
     * @return userId
     */
    public String getUserIdFromAccessToken(String token) {
        return getUserIdFromToken(token);
    }

    /**
     * 从刷新令牌解析用户ID
     * @param refreshToken 刷新令牌
     * @return userId
     */
    public String getUserIdFromRefreshToken(String refreshToken) {
        return getUserIdFromToken(refreshToken);
    }

    /**
     * 从访问令牌解析用户名
     */
    public String getUsernameFromAccessToken(String token) {
        return getUsernameFromToken(token);
    }

    /**
     * 从刷新令牌解析用户名
     */
    public String getUsernameFromRefreshToken(String token) {
        return getUsernameFromToken(token);
    }

    /**
     * 从请求头中提取 Token（标准格式：Bearer token）
     * @param request 请求对象
     * @return token 或 null
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(this.header);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // 去掉 "Bearer " 前缀
        }
        return null;
    }

    /**
     * 获取访问令牌过期时间（秒）
     */
    public Integer getAccessTokenExpireSeconds() {
        return accessTokenExpire;
    }

    // ============================== 内部私有方法 ==============================

    /**
     * 统一生成 JWT Token（核心方法）
     * @param userId 用户ID
     * @param expire 过期时间（秒）
     * @return JWT 字符串
     */
    private String generateToken(String userId, long expire) {
        Assert.hasText(userId, "用户ID不能为空");
        Assert.isTrue(expire > 0, "有效期必须大于0");

        // 自定义载荷（存放用户信息）
        Map<String, Object> claims = new HashMap<>(1);
        claims.put("userId", userId);

        // 过期时间
        Date expiration = new Date(System.currentTimeMillis() + expire * 1000L);
        SecretKey secretKey = getSecretKey();

        // 构建 JWT
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        log.debug("为用户[{}]生成JWT令牌: {}", userId, token);
        return token;
    }

    /**
     * 生成带用户名的 Token（高性能版，避免请求查库）
     */
    private String generateToken(String userId, String username, long expire) {
        Assert.hasText(userId, "用户ID不能为空");
        Assert.isTrue(expire > 0, "有效期必须大于0");

        Map<String, Object> claims = new HashMap<>(2);
        claims.put("userId", userId);
        claims.put("username", username);

        Date expiration = new Date(System.currentTimeMillis() + expire * 1000L);
        SecretKey secretKey = getSecretKey();

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        log.debug("为用户[{}]-[{}]生成JWT令牌: {}", userId, username, token);
        return token;
    }

    /**
     * 统一验证 Token 有效性
     * 捕获所有 JWT 异常，确保不抛错
     */
    private boolean validateToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Date expiration = claims.getExpiration();

            if (expiration == null) {
                log.warn("JWT令牌缺少过期时间");
                return false;
            }

            // 未过期 = 有效
            return !expiration.before(new Date());

        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT签名验证失败: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT令牌格式错误: {}", e.getMessage());
        } catch (InvalidClaimException e) {
            log.warn("JWT声明无效: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT令牌验证失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从 Token 提取用户ID
     */
    private String getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("userId", String.class));
    }

    /**
     * 从 Token 提取用户名
     */
    private String getUsernameFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("username", String.class));
    }

    /**
     * 通用提取声明方法
     */
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            log.error("从JWT令牌获取声明失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 Token 获取所有载荷（Claims）
     */
    private Claims getAllClaimsFromToken(String token) {
        Assert.hasText(token, "令牌不能为空");
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取 HMAC-SHA256 签名密钥
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(tokenSignKey.getBytes(StandardCharsets.UTF_8));
    }
}