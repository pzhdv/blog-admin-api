package cn.pzhdv.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * 系统角色表
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-01 22:00:59
 */
@Getter
@Setter
@ToString
@TableName("sys_role")
@Accessors(chain = true)
@ApiModel(value = "SysRole对象", description = "系统角色表")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID（主键）
     */
    @ApiModelProperty("角色ID（主键）")
    @TableId(value = "role_id", type = IdType.AUTO)
    private Long roleId;

    /**
     * 角色编码（唯一标识，如：R_SUPER_ADMIN/R_ADMIN/R_GUEST）
     */
    @TableField("role_code")
    @ApiModelProperty("角色编码（唯一标识，如：R_SUPER_ADMIN/R_ADMIN/R_GUEST")
    private String roleCode;

    /**
     * 角色名称（如：：超级管理员/管理员/访客）
     */
    @TableField("role_name")
    @ApiModelProperty("角色名称（如：超级管理员/管理员/访客）")
    private String roleName;

    /**
     * 角色描述
     */
    @TableField("role_desc")
    @ApiModelProperty("角色描述")
    private String roleDesc;

    /**
     * 角色状态（1:正常 2:禁止）
     */
    @TableField("status")
    @ApiModelProperty("角色状态（1:正常 2:禁止）")
    private Byte status;


    /**
     * 默认首页ID
     * 关联页面/菜单表的主键ID，用于指定该角色登录后默认打开的首页
     */
    @TableField("default_home_page_id")
    @ApiModelProperty(value = "默认首页ID（关联页面/菜单表主键）", example = "1001", notes = "存储角色登录后默认打开的首页对应的页面/菜单ID")
    private Long defaultHomePageId;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

}
