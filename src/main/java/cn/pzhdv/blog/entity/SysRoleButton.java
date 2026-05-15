package cn.pzhdv.blog.entity;

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

/**
 * <p>
 * 角色-按钮权限关联表
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-06 13:39:50
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("sys_role_button")
@ApiModel(value = "SysRoleButton对象", description = "角色-按钮权限关联表")
public class SysRoleButton implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID
     */
    @TableField("role_id")
    @ApiModelProperty("角色ID")
    private Long roleId;

    /**
     * 按钮ID
     */
    @TableField("button_id")
    @ApiModelProperty("按钮ID")
    private Long buttonId;
}
