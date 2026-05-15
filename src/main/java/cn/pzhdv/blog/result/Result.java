package cn.pzhdv.blog.result;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应结果封装类
 * <p>
 * 核心设计目标：
 * 1. 标准化所有接口的返回格式，前端统一解析逻辑，降低对接成本；
 * 2. 支持泛型数据封装，适配不同接口的返回数据类型（对象、列表、分页等）；
 * 3. 通过状态码+提示信息，清晰反馈接口执行结果（成功/失败/业务异常）；
 * 4. 适配Swagger文档展示，字段说明和示例值便于前端理解。
 * <p>
 * 通用使用规范：
 * - 成功响应：code=200，message="操作成功"，data=业务数据；
 * - 失败响应：code=非200（如400/401/500），message=具体错误信息，data=null；
 * - 业务异常：code=自定义业务码（如10001），message=业务提示信息，data=可选补充数据。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 * @param <T> 响应数据的泛型类型，支持任意Java对象（如List、实体类、Map等）
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@ApiModel(value = "Result", description = "接口统一返回格式，包含状态码、提示信息和响应数据")
public class Result<T> {

    /**
     * 响应状态码
     * <p>
     * 规范：
     * - 200：请求处理成功；
     * - 4xx：客户端错误（如400参数错误、401未登录、403无权限）；
     * - 5xx：服务端错误（如500系统异常）；
     * - 1xxx：自定义业务异常码（如10001用户不存在、10002数据重复）。
     *
     * @example 200
     */
    @ApiModelProperty(value = "响应状态码（200表示成功，非200表示失败）", example = "200", required = true)
    private Integer code;

    /**
     * 响应提示信息
     * <p>
     * 用于前端展示的友好提示，成功时返回"操作成功"，失败时返回具体错误原因（如"参数格式错误"）。
     *
     * @example 请求成功
     */
    @ApiModelProperty(value = "响应提示信息（前端可直接展示）", example = "请求成功", required = true)
    private String message;

    /**
     * 响应数据体
     * <p>
     * 接口处理成功时返回的业务数据，支持任意类型（列表、对象、分页数据等）；
     * 接口失败时该字段为null，仅通过code和message反馈错误信息。
     */
    @ApiModelProperty(value = "响应业务数据（接口成功时返回，失败时为null）")
    private T data;
}