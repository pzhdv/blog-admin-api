package cn.pzhdv.blog.utils;


import cn.pzhdv.blog.constant.RedisKey;

import java.util.Random;

/**
 * 缓存过期时间工具类
 * 统一生成带随机偏移量的过期时间，避免缓存雪崩
 * 基础时间和随机范围从RedisKey常量类获取，便于集中配置
 */
public class CacheExpireUtil {

    private static final Random RANDOM = new Random();

    /**
     * 生成默认过期时间（基础时间+随机偏移量）
     * 基础时间：RedisKey.BASE_EXPIRE_SECONDS（30分钟）
     * 随机范围：0~RedisKey.RANDOM_EXPIRE_SECONDS（10分钟）
     * @return 最终过期时间（秒）
     */
    public static int getDefaultExpireSeconds() {
        return calculateExpire(RedisKey.BASE_EXPIRE_SECONDS, RedisKey.RANDOM_EXPIRE_SECONDS);
    }

    /**
     * 生成自定义过期时间（基础时间+随机偏移量）
     * 用于特殊场景（如不同缓存需要不同的基础时间）
     * @param baseSeconds 基础过期时间（秒）
     * @param randomRangeSeconds 随机偏移范围（秒），实际偏移为0~randomRangeSeconds
     * @return 最终过期时间（秒）
     */
    public static int getCustomExpireSeconds(int baseSeconds, int randomRangeSeconds) {
        // 校验参数合法性，避免负数
        if (baseSeconds <= 0) {
            baseSeconds = RedisKey.BASE_EXPIRE_SECONDS;
        }
        if (randomRangeSeconds <= 0) {
            randomRangeSeconds = RedisKey.RANDOM_EXPIRE_SECONDS;
        }
        return calculateExpire(baseSeconds, randomRangeSeconds);
    }

    /**
     * 核心计算逻辑：基础时间 + 0~随机范围的偏移量
     */
    private static int calculateExpire(int baseSeconds, int randomRangeSeconds) {
        // 生成0~randomRangeSeconds之间的随机数（包含0，不包含randomRangeSeconds）
        int randomSeconds = RANDOM.nextInt(randomRangeSeconds);
        // 基础时间 + 随机偏移量
        return baseSeconds + randomSeconds;
    }
}
