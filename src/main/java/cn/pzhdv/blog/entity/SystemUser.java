package cn.pzhdv.blog.entity;

import cn.pzhdv.blog.vo.role.SysRoleSimpleVO;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 系统用户表
 * </p>
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("system_user")
@ApiModel(value = "SystemUser对象", description = "系统用户表")
public class SystemUser implements Serializable {

    private static final long serialVersionUID = 1L;

    // ------------------------------ 主键（分组校验：仅修改/删除需传） ------------------------------
    @ApiModelProperty(value = "系统用户ID（新增/登录无需传，修改/删除必须传正整数）", example = "1001", required = true)
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    // ------------------------------ 核心业务字段（严格校验） ------------------------------
    /**
     * 用户名- 数据库字段：user_name
     */
    @ApiModelProperty(value = "用户名（唯一，必填，4-20位字母/数字/下划线）", example = "admin123", required = true)
    @TableField("user_name")
    private String userName;

    /**
     * 登录密码
     */
    @ApiModelProperty(value = "登录密码（必填，8-32位，包含字母+数字+特殊字符）", example = "Admin@123456", required = true)
    @TableField("password")
    private String password;

    /**
     * 用户昵称
     */
    @ApiModelProperty(value = "用户昵称（必填，2-10位中文/字母/数字）", example = "潘总", required = true)
    @TableField("user_nick")
    private String userNick;

    /**
     * 手机号 - 数据库字段：user_phone
     */
    @ApiModelProperty(value = "手机号（唯一，11位纯数字）", example = "13800138000", required = true)
    @TableField("user_phone") // 修复：映射到数据库的user_phone字段
    private String userPhone;

    /**
     * 用户邮箱
     */
    @ApiModelProperty(value = "用户邮箱（唯一，格式需合法）", example = "admin@example.com")
    @TableField("user_email")
    private String userEmail;

    /**
     * 用户性别（1:男 2:女）
     */
    @ApiModelProperty(value = "用户性别（1:男 2:女，可选）", example = "1")
    private Byte userGender;

    /**
     * 用户状态（1:正常 2:禁止，默认1）
     */
    @ApiModelProperty(value = "用户状态（1:正常 2:禁止，默认1）", example = "1")
    @TableField("status")
    private Byte status;

    /**
     * 头像 头像URL（可选，需为有效URL）
     */
    @ApiModelProperty(value = "头像URL（可选，需为有效URL）", example = "https://example.com/avatar.png")
    @TableField("avatar")
    private String avatar;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间（自动填充，无需传值）", example = "2025-06-25 10:30:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "修改时间（自动填充，无需传值）", example = "2025-06-25 11:45:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // ------------------------------ 非数据库字段 ------------------------------
    /**
     * 用户关联的角色列表
     */
    @ApiModelProperty(value = "用户关联的角色列表（非数据库字段,查询时返回）", example = "[{\"roleId\":1,\"roleName\":\"管理员\",\"roleCode\":\"ADMIN\"},{\"roleId\":2,\"roleName\":\"普通用户\",\"roleCode\":\"USER\"}]")
    @TableField(exist = false)
    private List<SysRoleSimpleVO> roleList;

    /**
     * 用户关联的角色ID列表
     */
    @ApiModelProperty(value = "用户关联的角色ID列表（注册时可选绑定）", example = "[1,2,3]", required = false)
    @TableField(exist = false)
    private List<Long> roleIds;
}