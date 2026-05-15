package cn.pzhdv.blog.exception;


import cn.pzhdv.blog.config.FileSizeConfig;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.utils.IpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.sql.SQLSyntaxErrorException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.catalina.connector.ClientAbortException;
/**
 * 全局异常处理器
 * <p>
 * 核心设计目标：
 * 1. 统一捕获应用中所有未处理的异常，返回标准化的Result响应格式，避免前端解析混乱；
 * 2. 按异常类型精细化处理，区分「业务异常」「系统异常」「客户端异常」，返回精准的错误码和提示；
 * 3. 完善的日志记录策略：不同异常分级记录（DEBUG/WARN/ERROR），包含请求上下文（路径、方法、IP等），便于问题排查；
 * 4. 针对文件上传、参数校验、数据库交互等高频场景做专项优化，提升异常处理的精准度；
 * 5. 兼容Spring框架原生异常和自定义业务异常，保证异常处理的完整性。
 * <p>
 * 异常处理优先级：
 * - 自定义异常（BusinessException/FileValidationException）→ 框架原生异常 → 通用Exception（兜底）
 * <p>
 * 日志记录规范：
 * - DEBUG：客户端主动断开连接等无影响的异常，避免日志污染；
 * - WARN：客户端错误（参数、文件、404等），需关注但无需紧急处理；
 * - ERROR：服务端错误（SQL、数据访问、未知异常等），需及时排查修复。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  // 注入文件大小配置类，用于区分单文件/总文件超限
  private final FileSizeConfig fileSizeConfig;

  /** 捕获IO异常（包括连接重置等） 这类异常通常是网络问题或客户端问题，不是服务器业务错误 */
  @ExceptionHandler(IOException.class)
  public Result<?> handleIOException(IOException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    String message = e.getMessage();

    // 特定IO异常降级为DEBUG，避免日志污染
    if (message != null
        && (message.contains("你的主机中的软件中止了一个已建立的连接")
            || message.contains("Connection reset")
            || message.contains("Broken pipe")
            || message.contains("远程主机强迫关闭了一个现有的连接"))) {
      log.debug("IO异常(客户端断开): 路径={}, 方法={}, 消息={}", path, method, message);
      return null;
    }

    // 其他IO异常记录为WARN
    log.warn("IO异常: 路径={}, 方法={}, 消息={}", path, method, message);
    return ResultUtil.error(ResultCode.SERVER_ERROR);
  }

  /** 捕获所有未处理的异常（兜底处理） 当没有其他更具体的异常处理器匹配时，此方法将被调用 */
  @ExceptionHandler(Exception.class)
  public Result<?> handleUncaughtException(Exception e, HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    String clientIp = IpUtils.getClientIpForLog(request);
    String userAgent = request.getHeader("User-Agent");

    // 记录详细的异常信息，便于生产环境排查
    log.error(
        "兜底异常捕获: 路径={}, 方法={}, 客户端IP={}, UserAgent={}, 异常类型={}, 异常消息={}",
        path,
        method,
        clientIp,
        userAgent,
        e.getClass().getSimpleName(),
        e.getMessage(),
        e);
    return ResultUtil.error(ResultCode.SERVER_ERROR);
  }

  /** 捕获自定义业务异常 业务逻辑中抛出的异常，通常包含具体的错误代码和消息 */
  @ExceptionHandler(BusinessException.class)
  public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    int errorCode = e.getCode();
    String errorMsg = e.getMessage();
    log.error("业务异常捕获: 路径={}, 错误码={}, 消息={}", path, errorCode, errorMsg);

    // 判断消息是否有效
    if (errorMsg != null && !errorMsg.trim().isEmpty()) {
      return ResultUtil.error(errorCode, errorMsg);
    }
    // 若消息为空，使用默认的服务错误消息
    return ResultUtil.error(errorCode, ResultCode.SERVER_ERROR.message());
  }

  /** 捕获404异常（接口不存在） 当请求的接口路径不存在时，Spring会抛出此异常 */
  @ExceptionHandler(NoHandlerFoundException.class)
  public Result<?> handleNoHandlerFoundException(
      NoHandlerFoundException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();

    // 忽略浏览器自动请求的favicon.ico，避免日志污染
    if (path.endsWith("/favicon.ico")) {
      log.debug("404异常捕获(已忽略): 路径={}, 方法={}", path, method);
      return ResultUtil.error(ResultCode.NOT_FOUND);
    }

    log.error("404异常捕获(WARN): 路径={}, 方法={}, 消息={}", path, method, e.getMessage());
    return ResultUtil.error(ResultCode.NOT_FOUND);
  }

  /** 捕获请求方法不支持异常（405） 当请求的HTTP方法不被支持时，Spring会抛出此异常 */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public Result<?> handleMethodNotSupportedException(
      HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    log.error("405异常捕获: 路径={}, 方法={}, 支持方法={}", path, method, e.getSupportedHttpMethods());
    return ResultUtil.error(ResultCode.METHOD_NOT_ALLOWED);
  }

  /** 捕获SQL语法异常 对应 ResultCode 中“SQL语法错误” */
  @ExceptionHandler(SQLSyntaxErrorException.class)
  public Result<?> handleSQLSyntaxErrorException(
      SQLSyntaxErrorException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    log.error("SQL语法异常捕获: 路径={}, 消息={}", path, e.getMessage(), e);
    return ResultUtil.error(ResultCode.SQL_SYNTAX_ERROR);
  }

  /** 捕获数据完整性异常（唯一键约束、外键约束等） 优先处理唯一键约束异常，返回精准的错误提示 */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public Result<?> handleDataIntegrityViolationException(
      DataIntegrityViolationException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    String message = e.getMessage();
    log.warn("数据完整性异常捕获: 路径={}, 方法={}, 消息={}", path, method, message);

    // 检查是否为唯一键约束异常（Duplicate entry）
    if (message != null) {
      // MySQL格式: "Duplicate entry 'xxx' for key 'xxx'"
      // 或其他数据库的类似格式
      if (message.contains("Duplicate entry") || message.contains("duplicate key")
          || message.contains("唯一约束") || message.contains("UNIQUE constraint")) {
        // 提取重复的数据值
        String duplicateValue = extractDuplicateValue(message);
        if (duplicateValue != null && !duplicateValue.isEmpty()) {
          log.warn("唯一键约束异常: 路径={}, 重复数据={}", path, duplicateValue);
          return ResultUtil.error(ResultCode.DUPLICATE_DATA, duplicateValue);
        }
        // 无法提取具体值时，返回通用唯一键错误
        return ResultUtil.error(ResultCode.DATA_DUPLICATE, "1");
      }
    }

    // 其他数据完整性异常（如外键约束），根据HTTP方法返回相应错误
    String upperMethod = method.toUpperCase();
    switch (upperMethod) {
      case "POST":
        return ResultUtil.error(ResultCode.ADD_FAIL);
      case "PUT":
        return ResultUtil.error(ResultCode.UPDATE_FAIL);
      case "DELETE":
        return ResultUtil.error(ResultCode.DELETE_FAIL);
      default:
        return ResultUtil.error(ResultCode.DATA_INTEGRITY_ERROR);
    }
  }

  /** 捕获数据访问异常（数据库交互错误） 细分"查询/添加/修改/删除"失败场景，无明确场景则用通用服务错误 */
  @ExceptionHandler(DataAccessException.class)
  public Result<?> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    String message = e.getMessage();
    log.error("数据访问异常捕获: 路径={}, 方法={}, 消息={}", path, method, message, e);

    String upperMethod = method.toUpperCase();
    switch (upperMethod) {
      case "GET":
      case "SELECT": // 多case合并，用break跳出
        return ResultUtil.error(ResultCode.QUERY_FAIL);
      case "POST":
        return ResultUtil.error(ResultCode.ADD_FAIL);
      case "PUT":
        return ResultUtil.error(ResultCode.UPDATE_FAIL);
      case "DELETE":
        return ResultUtil.error(ResultCode.DELETE_FAIL);
      default: // 默认分支返回通用服务错误
        return ResultUtil.error(ResultCode.SERVER_ERROR);
    }
  }

  /** 捕获参数校验异常（@Valid注解校验失败） 支持动态填充“参数名”，如“刷新令牌不能为空” */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Result<?> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    // 获取第一个校验失败的字段和消息
    FieldError error = e.getBindingResult().getFieldError();
    String field = Objects.requireNonNull(error).getField();
    String defaultMsg = error.getDefaultMessage();
    log.warn("参数校验异常捕获: 路径={}, 字段={}, 消息={}", path, field, defaultMsg);

    // 若消息包含占位符，填充字段名；否则用默认消息
    if (defaultMsg != null && defaultMsg.contains("{}")) {
      return ResultUtil.error(ResultCode.PARAM_REQUIRED, field);
    }
    return ResultUtil.error(ResultCode.PARAM_INVALID.getCode(), defaultMsg);
  }

  /** 捕获缺失必填参数异常（如 @RequestParam 未传值） */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public Result<?> handleMissingParamException(
      MissingServletRequestParameterException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    String paramName = e.getParameterName();
    log.warn("缺失必填参数异常捕获: 路径={}, 缺失参数={}", path, paramName);
    // 动态填充"参数名"到错误消息
    return ResultUtil.error(ResultCode.PARAM_REQUIRED, paramName);
  }

  /** 捕获参数校验异常（ConstraintViolationException，用于方法参数级别验证） 支持 @PathVariable、@RequestParam 等参数上的验证注解 */
  @ExceptionHandler(ConstraintViolationException.class)
  public Result<?> handleConstraintViolationException(
      ConstraintViolationException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    // 获取所有验证失败的约束违规信息
    String errorMessage = e.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining("; "));
    
    // 获取第一个违规的字段路径（参数名）
    String paramName = e.getConstraintViolations().stream()
        .findFirst()
        .map(violation -> {
          String propertyPath = violation.getPropertyPath().toString();
          // 提取参数名（如 "updateUser.id" -> "id"）
          int lastDotIndex = propertyPath.lastIndexOf('.');
          return lastDotIndex >= 0 ? propertyPath.substring(lastDotIndex + 1) : propertyPath;
        })
        .orElse("参数");
    
    log.warn("参数校验异常捕获(ConstraintViolation): 路径={}, 参数={}, 消息={}", path, paramName, errorMessage);
    
    // 如果错误消息包含"不能为空"或"不能为null"，使用PARAM_REQUIRED
    if (errorMessage != null && (errorMessage.contains("不能为空") || errorMessage.contains("不能为null") 
        || errorMessage.contains("must not be null") || errorMessage.contains("must not be empty"))) {
      return ResultUtil.error(ResultCode.PARAM_REQUIRED, paramName);
    }
    
    // 其他验证失败场景，返回具体的验证消息
    return ResultUtil.error(ResultCode.PARAM_INVALID.getCode(), errorMessage);
  }

  /** 捕获JSON解析异常（请求体JSON格式错误） 涵盖“JSON解析异常”“消息不可读”（如类型不匹配）场景 */
  @ExceptionHandler({JsonProcessingException.class, HttpMessageNotReadableException.class})
  public Result<?> handleJsonParseException(Exception e, HttpServletRequest request) {
    String path = request.getRequestURI();
    log.warn("JSON解析异常捕获: 路径={}, 消息={}", path, e.getMessage());
    return ResultUtil.error(ResultCode.JSON_PARSE_ERROR);
  }

  /** 处理文件验证异常（自定义的FileValidationException） 涵盖：文件大小超限、文件类型不支持、文件流异常等 */
  @ExceptionHandler(FileValidationException.class)
  public Result<?> handleFileValidationException(
      FileValidationException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    log.warn("文件验证异常捕获 | 路径: {}, 错误码: {}, 错误消息: {}", path, e.getCode(), e.getMessage());

    // 自定义文件异常已包含精准消息，直接返回
    return ResultUtil.error(e.getCode(), e.getMessage());
  }

  /**
   * 处理Spring文件上传核心异常（MaxUploadSizeExceededException） 适配Spring Boot
   * 2.3.x版本，精准区分单文件/总文件超限，支持动态传递配置的最大大小
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public Result<?> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException e, HttpServletRequest request) {
    // 1. 基础参数获取
    String path = request.getRequestURI();
    String exceptionMsg = e.getMessage() != null ? e.getMessage().trim() : "";
    log.debug("文件超限异常原始信息：路径={}, 消息={}", path, exceptionMsg);

    // 2. 核心数据提取
    Long actualSize = extractActualSize(exceptionMsg); // 尝试从消息提取实际大小
    Long springLimitSize = extractSpringLimitSize(exceptionMsg); // Spring限制大小
    long configMaxFileSize = fileSizeConfig.getMaxFileSizeBytes(); // 配置的单文件大小
    long configTotalMaxSize = fileSizeConfig.getTotalMaxSizeBytes(); // 配置的总文件大小

    // 3. 格式转换（字节 → MB，保留1位小数）
    String configMaxMB = String.format("%.1fMB", configMaxFileSize / 1024.0 / 1024.0);
    String configTotalMB = String.format("%.1fMB", configTotalMaxSize / 1024.0 / 1024.0);
    String actualSizeDisplay; // 最终显示的实际大小描述

    // 4. 关键修复：处理实际大小为null的场景（单文件超限专属）
    if (actualSize != null) {
      // 能提取到实际大小（如总文件场景），正常显示MB
      actualSizeDisplay = String.format("%.1fMB", actualSize / 1024.0 / 1024.0);
    } else {
      // 提取不到实际大小 → 判断是否为单文件超限
      if (exceptionMsg.contains("The field file exceeds")
          || exceptionMsg.contains("FileSizeLimitExceededException")) {
        // 单文件超限：推断实际大小“超过配置值”（符合事实，用户更易理解）
        actualSizeDisplay = "超过" + configMaxMB;
      } else if (exceptionMsg.contains("Maximum upload size")
          || exceptionMsg.contains("SizeLimitExceededException")) {
        // 总文件超限：即使提取失败，也显示“未知”（总文件场景极少出现此情况）
        actualSizeDisplay = "未知";
      } else {
        // 其他场景：兜底显示“未知”
        actualSizeDisplay = "未知";
      }
    }

    // 5. 确定限制大小（配置值）的显示
    String displayLimitSize = "未知";
    if (springLimitSize != null) {
      displayLimitSize = (springLimitSize == configMaxFileSize) ? configMaxMB : configTotalMB;
    } else if (exceptionMsg.contains("The field file exceeds")) {
      displayLimitSize = configMaxMB; // 单文件超限特征
    } else if (exceptionMsg.contains("Maximum upload size")) {
      displayLimitSize = configTotalMB; // 总文件超限特征
    }

    // 6. 优化日志：实际请求大小不再显示“未知”，而是“超过10.0MB”
    log.warn(
        "文件大小超限: 路径={}, 限制大小={}（配置值）, 实际请求大小={}, Spring限制大小={}",
        path,
        displayLimitSize,
        actualSizeDisplay,
        springLimitSize != null ? springLimitSize + "B" : "未知");

    // 7. 单文件超限响应（动态消息）
    if (exceptionMsg.contains("The field file exceeds")
        || exceptionMsg.contains("FileSizeLimitExceededException")) {
      String errorMsg = ResultCode.FILE_TOO_LARGE.message(configMaxMB);
      return ResultUtil.error(ResultCode.FILE_TOO_LARGE, errorMsg);
    }

    // 8. 总文件超限响应（动态消息）
    if (exceptionMsg.contains("Maximum upload size")
        || exceptionMsg.contains("SizeLimitExceededException")) {
      String errorMsg = ResultCode.FILE_TOTAL_MAX_ERROR.message(configTotalMB);
      return ResultUtil.error(ResultCode.FILE_TOTAL_MAX_ERROR, errorMsg);
    }

    // 9. 兜底：未知场景返回通用错误
    log.error("未匹配到超限类型，路径={}, 消息={}", path, exceptionMsg);
    return ResultUtil.error(ResultCode.FILE_UPLOAD_FAIL);
  }

  /** 处理其他文件上传相关异常（MultipartException） 涵盖：空文件、不支持的媒体类型等场景 */
  @ExceptionHandler(MultipartException.class)
  public Result<?> handleMultipartException(MultipartException e, HttpServletRequest request) {
    String path = request.getRequestURI();
    String message = e.getMessage() != null ? e.getMessage() : "";
    log.warn("文件上传异常: 路径={}, 消息={}", path, message);

    // 1. 空文件处理
    if (message.contains("Empty file") || message.contains("文件为空")) {
      return ResultUtil.error(ResultCode.FILE_NULL_ERROR);
    }

    // 2. 不支持的文件类型处理
    if (message.contains("Unsupported media type") || message.contains("不支持的媒体类型")) {
      String fileType = extractFileType(message);
      return ResultUtil.error(ResultCode.UNSUPPORTED_FILE_TYPE, fileType);
    }

    // 3. 其他文件上传异常（如请求格式错误、临时文件创建失败等）
    return ResultUtil.error(ResultCode.FILE_UPLOAD_FAIL);
  }

  // ------------------------------ 工具方法 ------------------------------

  /**
   * 从异常消息中提取文件上传的实际大小（字节） 适配单文件和总文件超限的多种异常消息格式，解决不同场景下实际大小提取问题
   *
   * <p>核心逻辑：通过正则匹配从异常消息中提取实际上传的文件大小， 支持单文件超限（如"Requested size: 11534336 bytes"）和总文件超限（如"size
   * (150223872) exceeds"）等场景， 若所有格式均不匹配，返回null并打印警告日志，便于后续补充新格式的正则匹配
   *
   * @param exceptionMsg 异常消息字符串（来自MaxUploadSizeExceededException或MultipartException）
   * @return 提取到的实际文件大小（字节），提取失败则返回null
   */
  private Long extractActualSize(String exceptionMsg) {
    if (exceptionMsg == null || exceptionMsg.isEmpty()) {
      return null;
    }

    // 场景1：单文件超限标准格式 → "Requested size: 11534336 bytes"
    Matcher standardSingleMatcher =
        Pattern.compile("Requested size: (\\d+) bytes").matcher(exceptionMsg);
    if (standardSingleMatcher.find()) {
      return parseLongSafe(standardSingleMatcher.group(1));
    }

    // 场景2：单文件超限扩展格式 → "Uploaded size: 11534336 bytes"
    Matcher uploadSizeMatcher =
        Pattern.compile("Uploaded size: (\\d+) bytes").matcher(exceptionMsg);
    if (uploadSizeMatcher.find()) {
      return parseLongSafe(uploadSizeMatcher.group(1));
    }

    // 场景3：单文件超限简化格式 → "received 11534336 bytes"
    Matcher receivedSizeMatcher = Pattern.compile("received (\\d+) bytes").matcher(exceptionMsg);
    if (receivedSizeMatcher.find()) {
      return parseLongSafe(receivedSizeMatcher.group(1));
    }

    // 场景4：总文件超限标准格式 → "size (150223872) exceeds"
    Matcher totalSizeMatcher = Pattern.compile("size \\((\\d+)\\) exceeds").matcher(exceptionMsg);
    if (totalSizeMatcher.find()) {
      return parseLongSafe(totalSizeMatcher.group(1));
    }

    // 场景5：极端格式（无bytes标识）→ "exception is ...: 11534336"
    Matcher extremeSizeMatcher = Pattern.compile("exception is ...: (\\d+)").matcher(exceptionMsg);
    if (extremeSizeMatcher.find()) {
      return parseLongSafe(extremeSizeMatcher.group(1));
    }

    // 所有格式均不匹配时，记录未覆盖的消息格式，便于后续优化
    log.warn("未提取到实际请求大小，当前消息格式未覆盖：{}", exceptionMsg);
    return null;
  }

  /**
   * 从异常消息中提取Spring框架配置的文件大小限制（字节） 覆盖单文件和总文件超限的所有已知消息格式，确保能准确匹配配置的限制值
   *
   * <p>核心逻辑：通过正则匹配从异常消息中提取Spring实际应用的大小限制， 支持多种格式（如含"of"、无"of"、带括号等），解决Spring Boot 2.3.x中
   * 单文件超限场景e.getMaxUploadSize()返回-1的问题，为区分单文件/总文件超限提供依据
   *
   * @param exceptionMsg 异常消息字符串（来自MaxUploadSizeExceededException）
   * @return 提取到的Spring限制大小（字节），提取失败则返回null
   */
  private Long extractSpringLimitSize(String exceptionMsg) {
    if (exceptionMsg == null || exceptionMsg.isEmpty()) {
      return null;
    }

    // 格式1：标准带of格式 → "of 104857600 bytes"（单文件/总文件通用）
    Matcher format1 = Pattern.compile("of (\\d+) bytes").matcher(exceptionMsg);
    if (format1.find()) {
      return parseLongSafe(format1.group(1));
    }

    // 格式2：无of的总文件格式 → "Maximum upload size 104857600 bytes"
    Matcher format2 = Pattern.compile("Maximum upload size (\\d+) bytes").matcher(exceptionMsg);
    if (format2.find()) {
      return parseLongSafe(format2.group(1));
    }

    // 格式3：带括号的配置值格式 → "configured maximum (104857600)"
    Matcher format3 = Pattern.compile("configured maximum \\((\\d+)\\)").matcher(exceptionMsg);
    if (format3.find()) {
      return parseLongSafe(format3.group(1));
    }

    // 格式4：带allowed的限制格式 → "exceeds the maximum (104857600) allowed"
    Matcher format4 = Pattern.compile("maximum \\((\\d+)\\) allowed").matcher(exceptionMsg);
    if (format4.find()) {
      return parseLongSafe(format4.group(1));
    }

    // 格式5：带limit of的限制格式 → "size exceeds limit of 104857600 bytes"
    Matcher format5 = Pattern.compile("limit of (\\d+) bytes").matcher(exceptionMsg);
    if (format5.find()) {
      return parseLongSafe(format5.group(1));
    }

    // 记录未覆盖的消息格式，便于后续补充正则
    log.debug("未提取到Spring限制大小，消息格式未覆盖：{}", exceptionMsg);
    return null;
  }

  /** 工具方法：安全解析Long，避免NumberFormatException */
  private Long parseLongSafe(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      log.warn("解析数字失败，值={}", value, e);
      return null;
    }
  }

  /**
   * 从异常消息中提取重复的数据值（唯一键约束异常）
   * 支持多种数据库的唯一键异常消息格式
   *
   * @param message 异常消息字符串
   * @return 提取到的重复数据值，提取失败则返回null
   */
  private String extractDuplicateValue(String message) {
    if (message == null || message.isEmpty()) {
      return null;
    }

    // MySQL格式1: "Duplicate entry 'admin' for key 'uk_username'"
    Matcher mysqlMatcher1 = Pattern.compile("Duplicate entry '([^']+)' for key").matcher(message);
    if (mysqlMatcher1.find()) {
      return mysqlMatcher1.group(1);
    }

    // MySQL格式2: "Duplicate entry 'admin@example.com' for key 'PRIMARY'"
    Matcher mysqlMatcher2 = Pattern.compile("Duplicate entry '([^']+)'").matcher(message);
    if (mysqlMatcher2.find()) {
      return mysqlMatcher2.group(1);
    }

    // PostgreSQL格式: "duplicate key value violates unique constraint"
    // 或其他格式，尝试提取引号内的值
    Matcher quoteMatcher = Pattern.compile("['\"]([^'\"]+)['\"]").matcher(message);
    if (quoteMatcher.find()) {
      return quoteMatcher.group(1);
    }

    // 中文格式: "唯一约束 'xxx'"
    Matcher chineseMatcher = Pattern.compile("唯一约束[：:]['\"]?([^'\"\\s]+)['\"]?").matcher(message);
    if (chineseMatcher.find()) {
      return chineseMatcher.group(1);
    }

    return null;
  }

  /** 从异常消息中提取文件类型 */
  private String extractFileType(String message) {
    if (message == null || message.isEmpty()) {
      return "未知类型";
    }

    // JDK 11兼容的正则表达式：匹配.xxx、image/xxx或application/xxx格式
    // 修正字符集表示方式，确保兼容性
    Pattern pattern = Pattern.compile("(\\.[a-zA-Z0-9]+|image/[^,]+|application/[^,]+)");
    Matcher matcher = pattern.matcher(message);

    // JDK 11中matcher.find()返回boolean，需显式判断
    if (matcher.find()) {
      // 确保group(1)不为null（JDK 11对分组null值处理更严格）
      String result = matcher.group(1);
      return result != null ? result : "未知类型";
    }
    return "未知类型";
  }
}
