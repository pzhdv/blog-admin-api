package cn.pzhdv.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class SkyrocAdminApiApplicationTests {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void contextLoads() {
    }

    /**
     * 清空 Redis 当前库所有缓存
     */
    @Test
    public void clearAllRedisCache() {
        redisTemplate.execute((RedisConnection connection) -> {
            connection.flushDb();
            return null;
        });
        System.out.println("✅ Redis 当前库所有缓存已清空！");
    }

    /**
     * 清空 Redis 【所有库】的全部缓存（慎用！）
     */
    @Test
    public void clearAllRedisDb() {
        redisTemplate.execute((RedisConnection connection) -> {
            // flushDb() = 清空当前库
            // flushAll() = 清空所有库（0~15号库全部清空）
            connection.flushAll();
            return null;
        });
        System.out.println("✅ Redis 【所有库】全部缓存已清空！");
    }
}