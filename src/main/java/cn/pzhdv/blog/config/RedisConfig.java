package cn.pzhdv.blog.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 纯 JSON 序列化，无类型信息，100% 安全，无反序列化漏洞
 *
 * @author PanZonghui
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 定义统一的 ObjectMapper Bean
     * 这样 Spring 会自动管理它的生命周期，并可以被 RedisUtils 注入
     */
    @Bean(name = "redisObjectMapper") // 显式命名
    public ObjectMapper redisObjectMapper() {
        // 配置 ObjectMapper（支持时间、兼容扩展字段）
        ObjectMapper objectMapper = new ObjectMapper();
        // 支持 Java 8 / JDK17 时间类型（LocalDateTime等）
        objectMapper.registerModule(new JavaTimeModule());
        // 遇到未知属性不报错（项目扩展字段时兼容）
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    /**
     * RedisTemplate 配置（安全、通用、生产推荐）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        // ==================== Key / HashKey 序列化 ====================
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        // ==================== Value / HashValue 序列化 ====================
        // 纯 JSON 序列化，不存储 Java 类名 → 无安全漏洞
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

        jsonSerializer.setObjectMapper(redisObjectMapper);
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        // 初始化完成
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}