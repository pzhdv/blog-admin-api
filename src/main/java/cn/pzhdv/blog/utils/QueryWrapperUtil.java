package cn.pzhdv.blog.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

/**
 * MyBatis-Plus QueryWrapper 通用工具类
 * 特性：1. 过滤null/空字符串/全空格 2. 类型安全 3. 兼容MP的SFunction 4. 通用复用
 */
public class QueryWrapperUtil {

    // ====================== 基础空值判断（私有复用） ======================
    /**
     * 通用判断：值是否为有效非空（过滤null/空字符串/全空格）
     * @param value 待判断值
     * @return true=有效值，false=无效值
     */
    private static <V> boolean isEffectiveValue(V value) {
        if (value == null) {
            return false;
        }
        // 字符串类型：过滤空/全空格
        if (value instanceof String strValue) {
            return StringUtils.isNotBlank(strValue); // 直接复用MP的工具类，更统一
        }
        // 非字符串类型：非null即有效
        return true;
    }

    // ====================== 对外暴露的扩展方法 ======================

    /**
     * Lambda 模糊查询条件（仅支持字符串字段）
     * @param wrapper Lambda查询构造器
     * @param column  实体字段（SFunction）
     * @param value   模糊匹配值
     * @param <T>     实体类泛型
     */
    public static <T> void addLambdaLikeCondition(LambdaQueryWrapper<T> wrapper,
                                                  SFunction<T, String> column,
                                                  String value) {
        // 1. 先判空，再去首尾空格，最后判断是否为有效字符串
        String trimValue = null;
        if (value != null) {
            trimValue = value.trim();
            // 去空格后如果是空字符串，置为null
            if (trimValue.isEmpty()) {
                trimValue = null;
            }
        }
        // 2. 仅当去空格后为有效字符串时，拼接模糊查询条件
        if (trimValue != null) {
            wrapper.like(column, trimValue);
        }
    }

    /**
     * Lambda 等值查询条件（通用类型字段）
     * @param wrapper Lambda查询构造器
     * @param column  实体字段（SFunction）
     * @param value   匹配值
     * @param <T>     实体类泛型
     * @param <V>     字段值类型
     */
    public static <T, V> void addLambdaEqCondition(LambdaQueryWrapper<T> wrapper,
                                                   SFunction<T, V> column,
                                                   V value) {
        if (isEffectiveValue(value)) {
            wrapper.eq(column, value);
        }
    }

    // ====================== 补充常用扩展方法（可选但实用） ======================

    /**
     * Lambda 大于等于条件（>=）
     */
    public static <T, V> void addLambdaGeCondition(LambdaQueryWrapper<T> wrapper,
                                                   SFunction<T, V> column,
                                                   V value) {
        if (isEffectiveValue(value)) {
            wrapper.ge(column, value);
        }
    }

    /**
     * Lambda 小于等于条件（<=）
     */
    public static <T, V> void addLambdaLeCondition(LambdaQueryWrapper<T> wrapper,
                                                   SFunction<T, V> column,
                                                   V value) {
        if (isEffectiveValue(value)) {
            wrapper.le(column, value);
        }
    }

    /**
     * Lambda 非等值查询（!=）
     */
    public static <T, V> void addLambdaNeCondition(LambdaQueryWrapper<T> wrapper,
                                                   SFunction<T, V> column,
                                                   V value) {
        if (isEffectiveValue(value)) {
            wrapper.ne(column, value);
        }
    }

    /**
     * Lambda IN查询（集合类型）
     * @param value 集合（List/Set等），空集合/空值均过滤
     */
    public static <T, V> void addLambdaInCondition(LambdaQueryWrapper<T> wrapper,
                                                   SFunction<T, V> column,
                                                   Iterable<V> value) {
        if (value != null && value.iterator().hasNext()) {
            wrapper.in(column, value);
        }
    }

    // 私有化构造器：禁止实例化工具类
    private QueryWrapperUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}