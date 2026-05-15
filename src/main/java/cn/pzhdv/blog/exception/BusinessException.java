package cn.pzhdv.blog.exception;

import cn.pzhdv.blog.result.ResultCode;
import lombok.Getter;

/**
 * 自定义业务异常类
 * <p>
 * 核心设计目标：
 * 1. 区分「业务异常」与「系统异常」，便于全局异常处理器精准识别并返回标准化响应；
 * 2. 与ResultCode枚举强绑定，保证错误码/消息的统一性，避免硬编码；
 * 3. 支持动态填充错误消息占位符，适配个性化业务提示（如「用户【张三】不存在」）；
 * 4. 可选关闭异常堆栈跟踪，降低高频业务异常的性能损耗。
 * <p>
 * 使用场景：
 * - 业务逻辑校验失败（如用户不存在、参数不合法、权限不足）；
 * - 预期内的操作失败（如文件上传超限、数据重复）；
 * - 需向前端返回明确错误码和提示的场景。
 * <p>
 * 注意：系统异常（如NPE、SQL异常、IO异常）仍抛出原生异常，由全局异常处理器统一封装。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误状态码（与ResultCode枚举一致，前端可根据该码做差异化展示/处理）
     */
    private final int code;

    /**
     * 错误消息（支持动态占位符填充，前端可直接展示的友好提示文本）
     */
    private final String message;

    /**
     * 构造方法：直接传入ResultCode枚举（无动态参数）
     * <p>
     * 适用于错误消息无需个性化填充的通用业务异常场景。
     * 示例：
     * <pre>
     * // 抛出用户未登录异常
     * throw new BusinessException(ResultCode.NOT_LOGIN);
     * // 抛出请求参数错误通用异常
     * throw new BusinessException(ResultCode.BAD_REQUEST);
     * </pre>
     *
     * @param resultCode 预定义的状态码枚举（包含标准化的code和message）
     */
    public BusinessException(ResultCode resultCode) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 构造方法：传入ResultCode枚举 + 动态参数（填充占位符）
     * <p>
     * 适用于错误消息包含【{}】占位符，需要动态填充具体内容的场景。
     * 示例：
     * <pre>
     * // 抛出用户不存在异常（填充用户名）
     * throw new BusinessException(ResultCode.USER_NOT_EXIST, "张三");
     * // 抛出必传参数缺失异常（填充字段名）
     * throw new BusinessException(ResultCode.PARAM_REQUIRED, "手机号");
     * // 抛出文件大小超限异常（填充最大限制值）
     * throw new BusinessException(ResultCode.FILE_TOO_LARGE, "10MB");
     * </pre>
     *
     * @param resultCode 预定义的状态码枚举（包含带占位符的message）
     * @param params 动态参数（顺序与占位符一一对应，支持任意类型，自动转为字符串）
     */
    public BusinessException(ResultCode resultCode, Object... params) {
        this.code = resultCode.getCode();
        this.message = resultCode.message(params); // 调用枚举的message方法填充占位符
    }

    /**
     * 构造方法：传入自定义状态码和消息（极少使用）
     * <p>
     * 仅适用于「无法适配ResultCode枚举」的特殊业务场景，如：
     * 1. 第三方服务返回的非标准化错误码/消息；
     * 2. 临时的、无需纳入枚举管理的业务异常；
     * <p>
     * 注意：生产环境应尽量避免使用，优先扩展ResultCode枚举保证错误码规范。
     * 示例：
     * <pre>
     * // 抛出第三方AI服务自定义异常
     * throw new BusinessException(900001, "AI服务调用失败：" + aiErrorMsg);
     * </pre>
     *
     * @param code 自定义错误状态码（建议遵循「主码+子码」格式，如900001）
     * @param message 自定义错误消息（可动态拼接任意文本）
     */
    public BusinessException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 重写fillInStackTrace方法，关闭异常堆栈跟踪
     * <p>
     * 设计思路：
     * - 业务异常属于「预期异常」，通常不需要堆栈信息定位问题；
     * - 关闭堆栈填充可减少异常抛出时的性能损耗，提升高频异常场景的响应速度；
     * - 若需调试排查问题，可临时注释该方法恢复堆栈跟踪。
     *
     * @return Throwable 当前异常实例（不填充堆栈信息）
     */
    @Override
    public Throwable fillInStackTrace() {
        return this; // 不填充堆栈信息，直接返回当前实例
    }
}