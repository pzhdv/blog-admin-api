package cn.pzhdv.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 文件上传大小配置解析类
 * <p>
 * 核心功能：
 * 1. 从Spring配置文件（application.properties/yml）注入文件上传大小配置项；
 * 2. 提供配置值的单位转换能力，将易读的大小字符串（如10MB、500KB）解析为字节数；
 * 3. 对外暴露单文件最大大小、总请求文件大小的字节数获取方法，便于业务层校验文件大小。
 * <p>
 * 配置项说明：
 * - spring.servlet.multipart.max-file-size：单个上传文件的最大大小（默认10MB）；
 * - spring.servlet.multipart.max-request-size：单次请求所有上传文件的总大小（默认100MB）。
 * <p>
 * 支持的单位格式：
 * - 无单位：默认字节（B），如"1024" → 1024B；
 * - KB：千字节，1KB = 1024B；
 * - MB：兆字节，1MB = 1024KB；
 * - GB：吉字节，1GB = 1024MB；
 * 注：单位不区分大小写（如10mb、10MB均支持），配置值需为数字+单位的格式（如10MB、500KB）。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Component
public class FileSizeConfig {

    /**
     * 单个上传文件的最大大小（从配置注入，默认10MB）
     * 对应配置项：spring.servlet.multipart.max-file-size
     */
    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSize;

    /**
     * 单次请求所有上传文件的总大小（从配置注入，默认100MB）
     * 对应配置项：spring.servlet.multipart.max-request-size
     */
    @Value("${spring.servlet.multipart.max-request-size:100MB}")
    private String maxRequestSize;

    /**
     * 获取单个文件允许上传的最大大小（字节数）
     * <p>
     * 业务层可通过该方法获取配置的单文件上限，用于文件上传前的大小校验，
     * 避免触发Spring内置的文件大小超限异常。
     *
     * @return long 单文件最大大小（字节），如10MB → 10485760字节
     */
    public long getMaxFileSizeBytes() {
        return parseSizeToBytes(maxFileSize);
    }

    /**
     * 获取单次请求允许上传的文件总大小（字节数）
     * <p>
     * 适用于多文件批量上传场景，校验所有文件总大小不超过配置上限。
     *
     * @return long 总文件最大大小（字节），如100MB → 104857600字节
     */
    public long getTotalMaxSizeBytes() {
        return parseSizeToBytes(maxRequestSize);
    }

    /**
     * 核心解析方法：将配置的大小字符串转换为字节数
     * <p>
     * 解析规则：
     * 1. 去除字符串空格并转为大写，统一处理单位格式；
     * 2. 识别后缀单位（KB/MB/GB），计算对应的字节倍数；
     * 3. 解析数字部分并乘以单位倍数，得到最终字节数；
     * 4. 解析失败时抛出IllegalArgumentException，明确配置错误原因。
     *
     * @param size 配置的大小字符串（如"10MB"、"500KB"、"1024"）
     * @return long 转换后的字节数
     * @throws IllegalArgumentException 配置值格式无效时抛出（如非数字、未知单位）
     */
    private long parseSizeToBytes(String size) {
        // 空值处理：返回0字节（配置缺失时兜底）
        if (size == null || size.trim().isEmpty()) {
            return 0;
        }

        // 标准化处理：去除空格+转大写，避免单位大小写/空格导致解析失败
        String normalizedSize = size.trim().toUpperCase();
        long multiplier = 1; // 默认单位：字节（B）

        // 识别单位并设置对应倍数，同时截取数字部分
        if (normalizedSize.endsWith("KB")) {
            multiplier = 1024; // 1KB = 1024B
            normalizedSize = normalizedSize.substring(0, normalizedSize.length() - 2);
        } else if (normalizedSize.endsWith("MB")) {
            multiplier = 1024 * 1024; // 1MB = 1024KB
            normalizedSize = normalizedSize.substring(0, normalizedSize.length() - 2);
        } else if (normalizedSize.endsWith("GB")) {
            multiplier = 1024 * 1024 * 1024; // 1GB = 1024MB
            normalizedSize = normalizedSize.substring(0, normalizedSize.length() - 2);
        }

        // 解析数字部分并计算最终字节数
        try {
            long number = Long.parseLong(normalizedSize);
            return number * multiplier;
        } catch (NumberFormatException e) {
            // 包装异常，明确错误来源（配置值无效）
            throw new IllegalArgumentException("无效的文件大小配置值: " + size + "，请使用数字+单位格式（如10MB、500KB）", e);
        }
    }
}