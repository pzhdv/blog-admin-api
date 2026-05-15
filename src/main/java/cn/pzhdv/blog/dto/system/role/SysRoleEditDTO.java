package cn.pzhdv.blog.dto.system.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

/**
 * 系统角色修改请求DTO
 * <p>
 * 用于接收前端修改角色的请求参数，包含角色修改时允许变更的核心字段，
 * 排除 createTime（创建时间不允许修改）、updateTime（后端自动填充）、deleted（逻辑删除字段）等管控字段；
 * 所有参数均添加精准的校验规则，保证入参合法性，同时通过Swagger注解清晰说明字段使用规则。
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-01
 */
@Data
@ApiModel(value = "SysRoleEditDTO", description = "系统角色修改请求参数")
public class SysRoleEditDTO {

    /**
     * 角色ID（主键）
     * <p>修改场景下必填，用于定位要修改的角色，不可为空且必须为正整数</p>
     */
    @NotNull(message = "角色ID不能为空")
    @Positive(message = "角色ID必须为正整数")
    @ApiModelProperty(value = "角色ID（主键）",
            notes = "修改必填，用于定位待修改的角色，必须为正整数",
            required = true, example = "1")
    private Long roleId;

    /**
     * 角色编码（唯一标识）
     * <p>业务规则：仅允许大小写字母、数字、下划线，首字符为字母，长度4-16位，全局唯一</p>
     * <p>示例：ADMIN/operation_admin/GUEST_001</p>
     */
    @NotBlank(message = "角色编码不能为空")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,15}$",
            message = "角色编码仅允许大小写字母、数字、下划线，首字符为字母，长度4-16位"
    )
    @ApiModelProperty(
            value = "角色编码（唯一标识）",
            notes = "仅允许大小写字母、数字、下划线，首字符为字母，长度4-16位，全局唯一",
            required = true,
            example = "operation_admin"
    )
    private String roleCode;

    /**
     * 角色名称
     * <p>展示用名称，如：超级管理员/运营管理员/游客，支持中文、字母、数字，修改时不能为空</p>
     */
    @NotBlank(message = "角色名称不能为空")
    @ApiModelProperty(value = "角色名称",
            notes = "展示用名称，支持中文、字母、数字，如：超级管理员/运营管理员/游客",
            required = true, example = "运营管理员V2")
    private String roleName;

    /**
     * 角色描述
     * <p>角色的详细说明，用于备注角色的权限范围、适用场景等，非必填，可为空</p>
     */
    @ApiModelProperty(value = "角色描述",
            notes = "角色详细说明，备注权限范围、适用场景等，非必填",
            example = "负责系统运营模块的权限管理（V2版本，新增数据导出权限）")
    private String roleDesc;

    /**
     * 角色状态
     * <p>枚举值：1-正常（角色可用）、2-禁止（角色禁用）；修改时不能为空，仅允许传递这两个值</p>
     */
    @NotNull(message = "角色状态不能为空")
    @ApiModelProperty(value = "角色状态",
            notes = "1:正常（角色可用） 2:禁止（角色禁用），仅允许传递这两个值",
            required = true, example = "1", allowableValues = "1,2")
    private Byte status;

}