package cn.pzhdv.blog.vo.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 角色菜单信息VO
 * </p>
 * <p>
 * 用于返回角色拥有的菜单列表和首页菜单ID
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02
 */
@Data
@ApiModel(value = "RoleMenuVO", description = "角色菜单信息VO，包含菜单列表和首页菜单ID")
public class RoleMenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    @Min(value = 1, message = "角色ID必须为正整数")
    @ApiModelProperty(value = "角色ID", required = true, example = "1")
    private Long roleId;

    /**
     * 菜单Id列表
     */
    @ApiModelProperty(value = "菜单Id列表", example = "[1, 2, 3]")
    private List<Long> menuIdList;

    /**
     * 默认首页ID
     * 关联页面/菜单表的主键ID，用于指定该角色登录后默认打开的首页
     */
    @ApiModelProperty(value = "默认首页ID（关联页面/菜单表主键）", example = "1001", notes = "存储角色登录后默认打开的首页对应的页面/菜单ID，可为null")
    private Long defaultHomePageId;
}

