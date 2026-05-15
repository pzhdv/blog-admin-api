package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.SysMenuService;
import cn.pzhdv.blog.utils.RedisUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 健康检查控制器
 * </p>
 * <p>
 * 用于测试 MySQL 和 Redis 连接是否正常
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02
 */
@Slf4j
@Api(tags = "健康检查")
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final SysMenuService sysMenuService;
    private final RedisUtils redisUtils;

    @ApiLog("服务健康检查")
    @ApiOperation(value = "测试 MySQL 和 Redis 连接", notes = "测试 MySQL 数据库连接和 Redis 连接是否正常，返回测试结果", produces = "application/json")
    @GetMapping("/check")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 1. 测试 MySQL 连接
        Map<String, Object> mysqlResult = testMySQL();
        result.put("mysql", mysqlResult);

        // 2. 测试 Redis 连接
        Map<String, Object> redisResult = testRedis();
        result.put("redis", redisResult);

        // 3. 整体状态
        boolean allHealthy = (Boolean) mysqlResult.get("status") && (Boolean) redisResult.get("status");
        result.put("status", allHealthy ? "healthy" : "unhealthy");
        result.put("timestamp", currentTime);

        log.info("【健康检查】MySQL状态: {}, Redis状态: {}, 整体状态: {}",
                mysqlResult.get("status"), redisResult.get("status"), result.get("status"));

        if (allHealthy) {
            return ResultUtil.ok(result);
        } else {
            return ResultUtil.error(ResultCode.SERVER_ERROR, "健康检查失败", result);
        }
    }

    /**
     * 测试 MySQL 连接
     *
     * @return MySQL 测试结果
     */
    private Map<String, Object> testMySQL() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 执行一个简单的查询来测试数据库连接
            long startTime = System.currentTimeMillis();
            long count = sysMenuService.count();
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            result.put("status", true);
            result.put("message", "MySQL 连接正常");
            result.put("responseTime", responseTime + "ms");
            result.put("testQuery", "查询菜单表记录数");
            result.put("recordCount", count);
        } catch (Exception e) {
            log.error("【健康检查】MySQL 连接测试失败", e);
            result.put("status", false);
            result.put("message", "MySQL 连接失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        return result;
    }

    /**
     * 测试 Redis 连接
     *
     * @return Redis 测试结果
     */
    private Map<String, Object> testRedis() {
        Map<String, Object> result = new HashMap<>();
        try {
            String testKey = "health:check:test";
            String testValue = "health_check_" + System.currentTimeMillis();

            // 测试写入
            long startTime = System.currentTimeMillis();
            redisUtils.set(testKey, testValue, 60); // 设置60秒过期

            // 测试读取
            String readValue = redisUtils.get(testKey, String.class);
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            // 验证读取的值是否正确
            if (testValue.equals(readValue)) {
                // 清理测试数据
                redisUtils.del(testKey);

                result.put("status", true);
                result.put("message", "Redis 连接正常");
                result.put("responseTime", responseTime + "ms");
                result.put("testOperation", "写入和读取测试");
            } else {
                result.put("status", false);
                result.put("message", "Redis 读写数据不一致");
            }
        } catch (Exception e) {
            log.error("【健康检查】Redis 连接测试失败", e);
            result.put("status", false);
            result.put("message", "Redis 连接失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        return result;
    }
}