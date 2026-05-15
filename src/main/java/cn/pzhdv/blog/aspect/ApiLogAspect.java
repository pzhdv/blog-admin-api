package cn.pzhdv.blog.aspect;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.context.UserContext;
import cn.pzhdv.blog.entity.SysOperationLog;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.service.SysOperationLogService;
import cn.pzhdv.blog.utils.IpUtils;
import cn.pzhdv.blog.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 系统操作日志切面
 * 请求参数：完整不截断
 * IP获取：使用统一 IpUtils
 */
@Aspect
@Component
@Slf4j
public class ApiLogAspect {

    @Resource
    private SysOperationLogService sysLogService;

    /** 集合类型数据，仅记录前2条 */
    private static final int MAX_LIST_LOG_SIZE = 2;
    /** 响应内容最大长度 */
    private static final int MAX_DATA_LENGTH = 200;

    /**
     * 切点：匹配所有标注 @ApiLog 注解的方法
     */
    @Pointcut("@annotation(cn.pzhdv.blog.annotation.ApiLog)")
    public void logPointCut() {
    }

    /**
     * 环绕通知
     */
    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = (attributes != null) ? attributes.getRequest() : null;

        SysOperationLog logEntity = new SysOperationLog();
        fillBaseLogInfo(logEntity, point, request);

        try {
            // 执行目标接口
            Object result = point.proceed();
            // 处理响应结果
            handleResponseResult(logEntity, result);
            return result;
        } catch (Throwable e) {
            // 记录异常信息
            logEntity.setResponseResult("Exception: " + e.getMessage());
            throw e;
        } finally {
            // 填充耗时、时间并保存日志
            logEntity.setCostTime(System.currentTimeMillis() - startTime);
            logEntity.setCreateTime(new java.util.Date());
            sysLogService.saveLogAsync(logEntity);

            // 控制台输出日志
            log.info("[API LOG] {} {} | {}ms | IP: {}",
                    logEntity.getRequestMethod(),
                    logEntity.getRequestUrl(),
                    logEntity.getCostTime(),
                    logEntity.getIpAddress());
        }
    }

    /**
     * 填充日志基础信息
     */
    private void fillBaseLogInfo(SysOperationLog logEntity, ProceedingJoinPoint point, HttpServletRequest request) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        ApiLog apiLog = method.getAnnotation(ApiLog.class);

        // 接口描述
        logEntity.setDescription(apiLog != null ? apiLog.value() : "未命名接口");

        if (request != null) {
            logEntity.setRequestUrl(request.getRequestURI());
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setIpAddress(getClientIp(request));
            logEntity.setUserAgent(request.getHeader("User-Agent"));
            logEntity.setUsername(UserContext.getUsername());
        }

        // 参数名+参数值 成对封装
        Map<String, Object> paramMap = getStringObjectMap(point, signature);

        // 请求参数 不截断！
        String params = JsonUtils.objectToJson(paramMap);
        logEntity.setRequestParams(params);
    }

    /**
     * 构建请求参数名值对
     */
    private static Map<String, Object> getStringObjectMap(ProceedingJoinPoint point, MethodSignature signature) {
        String[] paramNames = signature.getParameterNames();
        Object[] args = point.getArgs();
        Map<String, Object> paramMap = new HashMap<>();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                Object arg = args[i];
                // 过滤无法序列化的参数
                if (arg instanceof HttpServletRequest
                        || arg instanceof HttpServletResponse
                        || arg instanceof MultipartFile) {
                    continue;
                }
                paramMap.put(paramNames[i], arg);
            }
        }
        return paramMap;
    }

    /**
     * 安全处理响应结果
     */
    private void handleResponseResult(SysOperationLog logEntity, Object result) {
        if (result instanceof Result<?> res) {
            Map<String, Object> logMap = new LinkedHashMap<>();
            logMap.put("code", res.getCode());
            logMap.put("message", res.getMessage());

            Object data = res.getData();
            if (data instanceof Collection<?> collection) {
                List<?> limitedList = collection.stream().limit(MAX_LIST_LOG_SIZE).toList();
                logMap.put("data", limitedList);
            } else {
                logMap.put("data", data);
            }

            // 最后统一序列化 + 截断
            String responseJson = JsonUtils.objectToJson(logMap);
            logEntity.setResponseResult(truncate(responseJson));
        } else {
            // 非标准返回体处理
            String json = JsonUtils.objectToJson(result);
            logEntity.setResponseResult(truncate(json));
        }
    }

    /**
     * 安全字符串截断
     */
    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= ApiLogAspect.MAX_DATA_LENGTH ? text : text.substring(0, ApiLogAspect.MAX_DATA_LENGTH) + "...[Truncated]";
    }

    /**
     * 获取客户端真实IP（使用统一IP工具类）
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        return IpUtils.getClientIpForLog(request);
    }

}