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
 * 系统按钮权限表
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-05 19:36:10
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("sys_button")
@ApiModel(value = "SysButton对象", description = "系统按钮权限表")
public class SysButton implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 按钮ID（主键）
     */
    @ApiModelProperty("按钮ID（主键）")
    @TableId(value = "button_id", type = IdType.AUTO)
    private Long buttonId;

    /**
     * 关联ID（sys_menu.menu_id）
     */
    @TableField("menu_id")
    @ApiModelProperty("关联ID（sys_menu.menu_id）")
    private Long menuId;

    /**
     * 按钮编码（唯一，如：btn:sys:user:add/btn:sys:user:edit）
     */
    @TableField("button_code")
    @ApiModelProperty("按钮编码（唯一，如：btn:sys:user:add/btn:sys:user:edit）")
    private String buttonCode;

    /**
     * 按钮名称（如：新增、编辑、删除）
     */
    @TableField("button_name")
    @ApiModelProperty("按钮名称（如：新增、编辑、删除）")
    private String buttonName;

    /**
     * 状态：1=启用 2=禁用
     */
    @TableField("status")
    @ApiModelProperty("状态：1=启用 2=禁用")
    private Byte status;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间（自动填充，无需传值）", example = "2025-06-25 10:30:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "修改时间（自动填充，无需传值）", example = "2025-06-25 11:45:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 名称（非数据库字段，用于关联查询返回）
     */
    @TableField(exist = false)
    @ApiModelProperty("名称")
    private String menuName;

    /**
     * 国际化key（非数据库字段，用于关联查询返回）
     */
    @TableField(exist = false)
    @ApiModelProperty("国际化key")
    private String menuI18nKey;

}
