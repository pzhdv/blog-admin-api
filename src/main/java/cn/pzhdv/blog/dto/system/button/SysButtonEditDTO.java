package cn.pzhdv.blog.dto.system.button;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

/**
 * 系统菜单按钮权限修改请求DTO
 * <p>
 * 用于接收前端修改按钮权限的请求参数，包含按钮权限修改时允许变更的核心字段，
 * 排除 createTime（创建时间不允许修改）、updateTime（后端自动填充）、deleted（逻辑删除字段）等管控字段；
 * 所有参数均添加精准的校验规则，保证入参合法性，同时通过Swagger注解清晰说明字段使用规则。
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-05
 */
@Data
@ApiModel(value = "SysButtonEditDTO", description = "系统菜单按钮权限修改请求参数")
public class SysButtonEditDTO {

    /**
     * 按钮ID（主键）
     * <p>修改场景下必填，用于定位要修改的按钮，不可为空且必须为正整数</p>
     */
    @NotNull(message = "按钮ID不能为空")
    @Positive(message = "按钮ID必须为正整数")
    @ApiModelProperty(value = "按钮ID（主键）",
            notes = "修改必填，用于定位待修改的按钮，必须为正整数",
            required = true, example = "1")
    private Long buttonId;

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
            notes = "全局唯一，用于权限标识，如 user:add、system:menu:add",
            required = true,
            example = "user:edit"
    )
    private String buttonCode;

    /**
     * 按钮名称
     * <p>展示用名称，如 新增、编辑、删除，支持中文、字母、数字，修改时不能为空</p>
     */
    @NotBlank(message = "按钮名称不能为空")
    @ApiModelProperty(value = "按钮名称",
            notes = "展示用名称，如 新增、编辑、删除",
            required = true, example = "编辑")
    private String buttonName;

    /**
     * 按钮状态
     * <p>枚举值：1-正常（按钮可用）、2-禁用（按钮不可用）；修改时不能为空，仅允许传递这两个值</p>
     */
    @NotNull(message = "按钮状态不能为空")
    @ApiModelProperty(value = "按钮状态",
            notes = "1:正常（可用） 2:禁用（不可用），仅允许传递这两个值",
            required = true, example = "1", allowableValues = "1,2")
    private Byte status;

}
