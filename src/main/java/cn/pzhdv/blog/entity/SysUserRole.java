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
 * 用户角色关联表（多对多）
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02 00:12:29
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("sys_user_role")
@ApiModel(value = "SysUserRole对象", description = "用户角色关联表（多对多）")
public class SysUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（关联system_user.user_id）
     */
    @TableField("user_id")
    @ApiModelProperty("用户ID（关联system_user.user_id）")
    private Long userId;

    /**
     * 角色ID（关联sys_role.role_id）
     */
    @TableField("role_id")
    @ApiModelProperty("角色ID（关联sys_role.role_id）")
    private Long roleId;
}
