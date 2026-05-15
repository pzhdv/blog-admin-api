package cn.pzhdv.blog.dto.system.button;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 系统菜单按钮权限新增请求DTO
 * <p>
 * 接收前端新增按钮权限的核心参数，仅包含需前端主动传递的字段，
 * 排除 createTime/updateTime/deleted/buttonId 等后端自动填充/数据库自增的字段，
 * 所有字段均添加校验规则，确保入参符合业务规范。
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-05
 */
@Data
@ApiModel(value = "SysButtonAddDTO", description = "系统菜单按钮权限新增请求参数")
public class SysButtonAddDTO {

    /**
     * 关联菜单ID
     * <p>业务规则：必须关联一个已存在的菜单ID</p>
     */
    @NotNull(message = "菜单ID不能为空")
    @ApiModelProperty(value = "关联菜单ID",
            notes = "必须关联一个已存在的菜单ID",
            required = true, example = "1")
    private Long menuId;

    /**
     * 按钮编码（唯一标识）
     * <p>业务规则：全局唯一，用于权限标识，如 user:add、system:menu:add，必须包含冒号</p>
     * <p>格式要求：字母开头，支持字母、数字、冒号、下划线，至少包含一个冒号</p>
     */
    @NotBlank(message = "按钮编码不能为空")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9:_]*:[a-zA-Z0-9:_]+$",
            message = "按钮编码格式错误，如 user:add 或 system:menu:add"
    )
    @ApiModelProperty(
            value = "按钮编码（唯一标识）",
            notes = "全局唯一，用于权限标识，如 user:add、user:edit、user:delete",
            required = true,
            example = "user:add"
    )
    private String buttonCode;

    /**
     * 按钮名称
     * <p>业务规则：展示用名称，支持中文、字母、数字，如 新增、编辑、删除</p>
     */
    @NotBlank(message = "按钮名称不能为空")
    @ApiModelProperty(value = "按钮名称",
            notes = "展示用名称，如 新增、编辑、删除",
            required = true, example = "新增")
    private String buttonName;

    /**
     * 按钮状态
     * <p>业务规则：1-正常（可用）、2-禁用（不可用），新增时建议默认传1</p>
     */
    @NotNull(message = "按钮状态不能为空")
    @ApiModelProperty(value = "按钮状态",
            notes = "1:正常（可用） 2:禁用（不可用），新增默认传1",
            required = true, example = "1", allowableValues = "1,2")
    private Byte status;

}
