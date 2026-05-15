package cn.pzhdv.blog.dto.system.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 系统角色新增请求DTO
 * <p>
 * 接收前端新增角色的核心参数，仅包含需前端主动传递的字段，
 * 排除 createTime/updateTime/deleted/roleId 等后端自动填充/数据库自增的字段，
 * 所有字段均添加校验规则，确保入参符合业务规范。
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-01
 */
@Data
@ApiModel(value = "SysRoleAddDTO", description = "系统角色新增请求参数")
public class SysRoleAddDTO {

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
     * <p>业务规则：展示用名称，支持中文、字母、数字，如 超级管理员/运营管理员/游客</p>
     */
    @NotBlank(message = "角色名称不能为空")
    @ApiModelProperty(value = "角色名称",
            notes = "展示用名称，支持中文、字母、数字，示例：超级管理员/运营管理员/游客",
            required = true, example = "运营管理员")
    private String roleName;

    /**
     * 角色描述
     * <p>业务规则：角色的详细说明，备注权限范围、适用场景等，非必填</p>
     */
    @ApiModelProperty(value = "角色描述",
            notes = "角色详细说明，备注权限范围、适用场景等，非必填",
            example = "负责系统运营模块的权限管理，包含数据查看、操作权限")
    private String roleDesc;

    /**
     * 角色状态
     * <p>业务规则：1-正常（可用）、2-禁止（禁用），新增时建议默认传1</p>
     */
    @NotNull(message = "角色状态不能为空")
    @ApiModelProperty(value = "角色状态",
            notes = "1:正常（可用） 2:禁止（禁用），新增默认传1",
            required = true, example = "1", allowableValues = "1,2")
    private Byte status;

}