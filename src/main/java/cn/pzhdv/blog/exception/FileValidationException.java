package cn.pzhdv.blog.exception;

import cn.pzhdv.blog.result.ResultCode;

/**
 * 文件校验专用业务异常类
 * <p>
 * 核心定位：
 * 1. 继承BusinessException，复用标准化错误码/消息机制，同时精准定位「文件相关校验失败」场景；
 * 2. 专用于文件上传/下载/删除等流程中的校验异常（如文件大小超限、类型不支持、文件不存在等）；
 * 3. 与ResultCode中文件相关枚举（如FILE_TOO_LARGE、UNSUPPORTED_FILE_TYPE）强绑定使用。
 * <p>
 * 使用场景：
 * - 文件上传时大小超限（单文件/总文件）；
 * - 文件类型不支持（如上传exe、bat等禁止类型）；
 * - 文件不存在（下载/删除时目标文件未找到）；
 * - 文件上传请求异常（如无文件、请求格式错误）；
 * - 文件删除失败（权限不足、文件被占用）。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 * @see BusinessException 基础业务异常父类
 * @see ResultCode 包含文件相关错误码（如413001 FILE_TOO_LARGE、415001 UNSUPPORTED_FILE_TYPE）
 */
public class FileValidationException extends BusinessException {

    /**
     * 构造方法：仅接收文件相关ResultCode枚举（无动态参数）
     * <p>
     * 适用于文件校验异常无需动态填充占位符的场景。
     * 示例：
     * <pre>
     * // 文件上传请求异常（无动态参数）
     * throw new FileValidationException(ResultCode.FILE_UPLOAD_FAIL);
     * // 上传文件为空（无动态参数）
     * throw new FileValidationException(ResultCode.FILE_NULL_ERROR);
     * </pre>
     *
     * @param resultCode 文件相关的状态码枚举（如ResultCode.FILE_UPLOAD_FAIL、ResultCode.FILE_NOT_FOUND）
     */
    public FileValidationException(ResultCode resultCode) {
        super(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 构造方法：接收文件相关ResultCode枚举 + 动态参数（填充占位符）
     * <p>
     * 适用于文件校验异常需要动态填充占位符的场景，是文件校验异常的核心构造方式。
     * 示例：
     * <pre>
     * // 单文件大小超限（填充最大限制值）
     * throw new FileValidationException(ResultCode.FILE_TOO_LARGE, "10MB");
     * // 不支持的文件类型（填充具体类型）
     * throw new FileValidationException(ResultCode.UNSUPPORTED_FILE_TYPE, ".exe");
     * // 文件不存在（填充文件名）
     * throw new FileValidationException(ResultCode.FILE_NOT_FOUND, "test.jpg");
     * </pre>
     *
     * @param resultCode 文件相关的状态码枚举（如ResultCode.FILE_TOO_LARGE、ResultCode.UNSUPPORTED_FILE_TYPE）
     * @param args 动态参数（与ResultCode消息中的【{}】占位符一一对应，如文件大小、文件类型、文件名）
     */
    public FileValidationException(ResultCode resultCode, Object... args) {
        super(resultCode.getCode(), resultCode.message(args));
    }
}