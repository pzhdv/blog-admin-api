package cn.pzhdv.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 系统接口访问日志表
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-13 17:46:48
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("sys_operation_log")
@ApiModel(value = "SysOperationLog对象", description = "系统接口访问日志表")
public class SysOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 登录用户名
     */
    @TableField("username")
    @ApiModelProperty("登录用户名")
    private String username;

    /**
     * 请求接口路径
     */
    @TableField("request_url")
    @ApiModelProperty("请求接口路径")
    private String requestUrl;

    /**
     * 请求方式 GET/POST/PUT/DELETE
     */
    @TableField("request_method")
    @ApiModelProperty("请求方式 GET/POST/PUT/DELETE")
    private String requestMethod;

    /**
     * 请求参数
     */
    @ApiModelProperty("请求参数")
    @TableField("request_params")
    private String requestParams;

    /**
     * 响应结果
     */
    @ApiModelProperty("响应结果")
    @TableField("response_result")
    private String responseResult;

    /**
     * 接口耗时(毫秒)
     */
    @TableField("cost_time")
    @ApiModelProperty("接口耗时(毫秒)")
    private Long costTime;

    /**
     * 访问IP地址
     */
    @TableField("ip_address")
    @ApiModelProperty("访问IP地址")
    private String ipAddress;

    /**
     * 浏览器/设备UA信息
     */
    @TableField("user_agent")
    @ApiModelProperty("浏览器/设备UA信息")
    private String userAgent;

    /**
     * 接口功能描述
     */
    @TableField("description")
    @ApiModelProperty("接口功能描述")
    private String description;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
}
