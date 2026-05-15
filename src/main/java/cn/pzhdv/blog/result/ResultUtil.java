package cn.pzhdv.blog.result;

/**
 * 接口响应结果构建工具类
 * <p>
 * 核心功能：
 * 1. 封装统一的响应结果构建逻辑，简化Controller层响应返回代码；
 * 2. 支持成功响应、不同场景的错误响应构建，适配ResultCode枚举的占位符特性；
 * 3. 提供灵活的错误响应重载方法，满足标准化/自定义错误提示的需求；
 * 4. 统一管理响应数据的格式，确保所有接口返回结构一致。
 * <p>
 * 使用规范：
 * - 成功响应：优先使用ok(T data)，统一返回200状态码 + "请求成功" + 业务数据；
 * - 标准化错误：优先使用error(ResultCode)或error(ResultCode, Object...)，基于枚举保证错误码/消息规范；
 * - 自定义错误：仅特殊场景使用error(int, String)（如第三方服务异常、非标准化错误）。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
public class ResultUtil {

    /**
     * 成功响应（带业务数据）
     * <p>
     * 所有接口处理成功且需要返回数据时使用，统一返回200状态码和"请求成功"提示。
     * 示例：
     * <pre>
     * // 返回用户列表数据
     * return ResultUtil.ok(userList);
     * // 返回单个用户对象
     * return ResultUtil.ok(user);
     * </pre>
     *
     * @param <T> 响应数据的泛型类型（支持List、实体类、Map等任意类型）
     * @param data 接口处理成功后的业务数据（可为null，此时返回空数据体）
     * @return Result<T> 统一成功响应对象（code=200，message="请求成功"，data=传入数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.message(), data);
    }

    /**
     * 错误响应（标准化枚举，无动态参数）
     * <p>
     * 接口处理失败且错误信息无需动态填充时使用，基于ResultCode枚举保证错误码/消息的规范性。
     * 示例：
     * <pre>
     * // 用户未登录
     * return ResultUtil.error(ResultCode.NOT_LOGIN);
     * // 请求参数错误（通用）
     * return ResultUtil.error(ResultCode.BAD_REQUEST);
     * </pre>
     *
     * @param <T> 响应数据类型（固定为null，泛型仅为适配Result<T>结构）
     * @param resultCode 错误状态码枚举（包含预定义的code和message）
     * @return Result<T> 统一错误响应对象（data=null）
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 错误响应（标准化枚举 + 动态参数，填充占位符）
     * <p>
     * 接口处理失败且错误信息需要动态填充占位符时使用，适配带【{}】占位符的ResultCode枚举。
     * 示例：
     * <pre>
     * // 必传参数缺失（填充字段名）
     * return ResultUtil.error(ResultCode.PARAM_REQUIRED, "手机号");
     * // 用户不存在（填充用户ID）
     * return ResultUtil.error(ResultCode.USER_NOT_EXIST, 1001);
     * // 多占位符填充（如数据重复提示）
     * return ResultUtil.error(ResultCode.DATA_DUPLICATE, 2);
     * </pre>
     *
     * @param <T> 响应数据类型（固定为null，泛型仅为适配Result<T>结构）
     * @param resultCode 错误状态码枚举（包含带占位符的message）
     * @param params 占位符填充参数（顺序与枚举message中的【{}】一一对应）
     * @return Result<T> 统一错误响应对象（message为填充后的完整信息，data=null）
     */
    public static <T> Result<T> error(ResultCode resultCode, Object... params) {
        return new Result<>(resultCode.getCode(), resultCode.message(params), null);
    }

    /**
     * 错误响应（自定义状态码 + 错误消息）
     * <p>
     * 仅用于特殊场景：
     * 1. 第三方服务返回的自定义错误码/消息，无法适配ResultCode枚举；
     * 2. 临时的、非标准化的错误提示，无需纳入枚举管理；
     * 3. 动态生成的错误消息（如运行时拼接的异常信息）。
     * <p>
     * 注意：生产环境应尽量避免使用，优先通过扩展ResultCode枚举保证错误码规范。
     * 示例：
     * <pre>
     * // 第三方AI服务调用失败（自定义码+消息）
     * return ResultUtil.error(900001, "AI服务返回异常：" + aiErrorMsg);
     * // 临时的业务异常（非枚举定义）
     * return ResultUtil.error(800001, "当前时段不支持该操作");
     * </pre>
     *
     * @param <T> 响应数据类型（固定为null，泛型仅为适配Result<T>结构）
     * @param code 自定义错误状态码（可使用非ResultCode定义的数字）
     * @param message 自定义错误消息（任意字符串，支持动态拼接）
     * @return Result<T> 统一错误响应对象（data=null）
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

}