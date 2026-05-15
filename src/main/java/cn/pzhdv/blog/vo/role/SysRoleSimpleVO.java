package cn.pzhdv.blog.vo.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "SysRoleSimpleVO", description = "角色精简信息VO，仅返回角色核心标识字段")
public class SysRoleSimpleVO {

    @ApiModelProperty(value = "角色ID", dataType = "Long", example = "1", notes = "角色唯一主键ID")
    private Long roleId;

    @ApiModelProperty(value = "角色编码", dataType = "String", example = "admin", notes = "角色唯一编码（如：admin/editor/guest）")
    private String roleCode;

    @ApiModelProperty(value = "角色名称", dataType = "String", example = "超级管理员", notes = "角色展示名称（如：超级管理员/普通编辑）")
    private String roleName;
}
