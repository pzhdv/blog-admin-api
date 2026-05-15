package cn.pzhdv.blog.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 通用工具类
 * 功能：封装 Redis 常用操作，支持字符串、对象、List、Set、Map、Hash、泛型类型反序列化
 * 说明：所有方法均增加异常日志打印，生产环境可用
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-06-25 13:10:16
 */
@Component
@Slf4j
public class RedisUtils {

    /**
     * Spring 自动注入 Redis 模板
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 使用 Qualifier 确保注入的是 RedisConfig 中定义的那个专用的 ObjectMapper
     */
    @Resource
    @Qualifier("redisObjectMapper")
    private ObjectMapper redisObjectMapper;

    // ==================== 公共通用方法 ====================


    /**
     * 统一的内部转换方法（解决所有 Long/Integer/LinkedHashMap 转换问题）
     */
    private <T> T convert(Object value, Class<T> clazz) {
        if (value == null) return null;
        return redisObjectMapper.convertValue(value, clazz);
    }

    /**
     * 指定缓存的过期时间（秒）
     *
     * @param key  缓存键
     * @param time 过期时间（秒）
     * @return 操作是否成功
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("Redis设置过期时间异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * 获取 key 的剩余过期时间
     *
     * @param key 缓存键
     * @return 剩余时间（秒），0 = 永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断 key 是否存在
     *
     * @param key 缓存键
     * @return true=存在，false=不存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis判断key是否存在异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * 删除缓存（支持单个/批量删除）
     *
     * @param key 一个或多个缓存键
     */
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(Arrays.asList(key));
            }
        }
    }

    /**
     * 根据通配符批量删除 key
     *
     * @param pattern 通配符，如 article:*、user:login:*、category:*
     */
    public void deleteKeysByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            // 分批迭代，不会阻塞 Redis
            List<String> keysToDelete = new ArrayList<>();
            int batchSize = 500;
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
                // 达到批量阈值时执行批量删除，减少网络往返
                if (keysToDelete.size() >= batchSize) {
                    redisTemplate.delete(keysToDelete);
                    keysToDelete.clear();
                }
            }
            // 处理剩余的 keys
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
        } catch (Exception e) {
            log.error("Redis scan批量删除key异常，pattern：{}", pattern, e);
        }
    }

    // ==================== String & Object 操作 ====================

    /**
     * 获取指定类型的缓存（普通对象/基本类型：String、Long、Integer、自定义实体类等）
     * 适用场景：单个对象、基本数据类型，无泛型
     *
     * @param key   缓存键
     * @param clazz 目标类型
     * @return 转换后的对象，获取失败返回 null
     * <p>
     * 使用案例：
     * Article article = redisUtils.get("article:1", Article.class);
     * Long count = redisUtils.get("article:count", Long.class);
     * String name = redisUtils.get("user:name", String.class);
     */
    public <T> T get(String key, Class<T> clazz) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }

            return convert(value, clazz);
        } catch (ClassCastException e) {
            log.error("Redis类型转换异常，key：{}", key, e);
            return null;
        } catch (Exception e) {
            log.error("Redis获取数据异常，key：{}", key, e);
            return null;
        }
    }

    /**
     * 【泛型专用】获取泛型类型缓存（支持 List<T>、Page<T>、Set<T> 等复杂泛型）
     * 适用场景：分页对象、集合列表、带泛型的所有结构
     *
     * @param key           缓存键
     * @param typeReference 泛型类型引用（用于保留泛型信息）
     * @return 泛型对象，获取失败返回 null
     * <p>
     * 使用案例（JDK17+ 简洁写法）：
     * List<Article> list = redisUtils.get("article:list", new TypeReference<>() {});
     * Page<Article> page = redisUtils.get("article:page", new TypeReference<>() {});
     * List<ArticleCategory> tree = redisUtils.get("category:tree", new TypeReference<>() {});
     */
    public <T> T get(String key, TypeReference<T> typeReference) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        try {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj == null) {
                return null;
            }
            // Jackson 安全地将 LinkedHashMap 反序列化为指定泛型类型
            return redisObjectMapper.convertValue(obj, typeReference);
        } catch (Exception e) {
            log.error("Redis泛型反序列化异常，key：{}", key, e);
            return null;
        }
    }

    /**
     * 存入缓存（永久有效）
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 操作是否成功
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis设置缓存异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * 存入缓存并设置过期时间
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param time  过期时间（秒），<=0 表示永久
     * @return 操作是否成功
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("Redis设置带过期时间缓存异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * 数值递增（可用于计数、统计）
     *
     * @param key   键
     * @param delta 增量（必须 > 0）
     * @return 递增后的值
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 数值递减
     *
     * @param key   键
     * @param delta 减量（必须 > 0）
     * @return 递减后的值
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    // ==================== Hash 操作 ====================


    /**
     * Hash 获取单个字段值（带自动转换）
     * 使用：User user = redisUtils.hget("user:map", "1", User.class);
     */
    public <T> T hget(String key, String item, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, item);
        return convert(value, clazz);
    }
    /**
     * Hash 获取所有键值对
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Hash 批量存入键值对
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("Redis Hash批量存入异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * Hash 批量存入并设置过期时间
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) expire(key, time);
            return true;
        } catch (Exception e) {
            log.error("Redis Hash批量存入带过期时间异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * Hash 存入单个字段
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            log.error("Redis Hash存入异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * Hash 存入单个字段并设置过期时间
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) expire(key, time);
            return true;
        } catch (Exception e) {
            log.error("Redis Hash存入带过期时间异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * Hash 删除字段
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断 Hash 中是否存在某个字段
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * Hash 字段递增
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * Hash 字段递减
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    // ==================== Set 操作 ====================

    /**
     * Set 获取所有成员
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Redis Set获取异常，key：{}", key, e);
            return null;
        }
    }

    /**
     * Set 判断值是否存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.error("Redis Set判断存在异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * Set 添加数据
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("Redis Set添加异常，key：{}", key, e);
            return 0;
        }
    }

    /**
     * Set 添加数据并设置过期时间
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) expire(key, time);
            return count;
        } catch (Exception e) {
            log.error("Redis Set添加带过期时间异常，key：{}", key, e);
            return 0;
        }
    }

    /**
     * Set 获取集合大小
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            log.error("Redis Set获取长度异常，key：{}", key, e);
            return 0;
        }
    }

    /**
     * Set 移除指定值
     */
    public long setRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            log.error("Redis Set删除异常，key：{}", key, e);
            return 0;
        }
    }

    // ==================== List 操作 ====================

    /**
     * List 获取范围（带自动转换）
     * 使用：List<User> list = redisUtils.lGet("user:list", 0, -1, User.class);
     */
    public <T> List<T> lGet(String key, long start, long end, Class<T> clazz) {
        try {
            List<Object> rawList = redisTemplate.opsForList().range(key, start, end);
            if (rawList == null) return Collections.emptyList();

            List<T> result = new ArrayList<>();
            for (Object item : rawList) {
                result.add(convert(item, clazz));
            }
            return result;
        } catch (Exception e) {
            log.error("Redis List获取异常，key：{}", key, e);
            return Collections.emptyList();
        }
    }

    /**
     * List 获取长度
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error("Redis List获取长度异常，key：{}", key, e);
            return 0;
        }
    }

    /**
     * List 根据索引获取值
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.error("Redis List索引获取异常，key：{}", key, e);
            return null;
        }
    }

    /**
     * List 右侧追加元素
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis List添加异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * List 右侧追加元素并设置过期时间
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) expire(key, time);
            return true;
        } catch (Exception e) {
            log.error("Redis List添加带过期时间异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * List 右侧批量追加集合
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis List批量添加异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * List 右侧批量追加集合并设置过期时间
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) expire(key, time);
            return true;
        } catch (Exception e) {
            log.error("Redis List批量添加带过期时间异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * List 根据索引修改值
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            log.error("Redis List修改索引值异常，key：{}", key, e);
            return false;
        }
    }

    /**
     * List 移除 N 个相同值
     *
     * @param count 移除数量
     */
    public long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Exception e) {
            log.error("Redis List移除元素异常，key：{}", key, e);
            return 0;
        }
    }
}