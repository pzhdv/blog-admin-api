package cn.pzhdv.blog.vo.auth;

import cn.pzhdv.blog.entity.SysMenu;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统菜单路由视图对象（VO）
 *
 * <p>用于返回用户可访问的系统菜单路由信息，包含菜单路由树形结构和默认首页路径。
 * 前端可根据此信息动态生成菜单导航和路由配置。
 *
 * <p>使用场景：
 * <ul>
 *   <li>用户登录后获取菜单权限</li>
 *   <li>角色权限变更后刷新菜单</li>
 *   <li>前端动态路由配置</li>
 * </ul>
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2026-01-02
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "SystemMenuRoute", description = "系统菜单路由视图对象，包含菜单路由树形结构和默认首页路径")
public class SystemMenuRoute implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 系统路由列表（树形结构）
     *
     * <p>包含用户有权限访问的所有菜单路由，以树形结构组织。
     * 每个菜单项包含路由路径、组件路径、图标、权限等信息。
     * 前端可根据此树形结构动态生成侧边栏菜单和路由配置。
     *
     * <p>路由树结构说明：
     * <ul>
     *   <li>一级菜单：目录类型（menuType=1），通常作为菜单分组</li>
     *   <li>二级菜单：菜单类型（menuType=2），对应具体的页面路由</li>
     *   <li>支持多级嵌套，通过children字段实现树形结构</li>
     * </ul>
     */
    @ApiModelProperty(
            value = "系统路由列表（树形结构）",
            required = true,
            example = "[{\"menuId\":1,\"menuName\":\"系统管理\",\"routePath\":\"/system\",\"children\":[{\"menuId\":2,\"menuName\":\"用户管理\",\"routePath\":\"/system/user\"}]}]",
            notes = "用户有权限访问的菜单路由树形结构，包含路由路径、组件路径、图标等信息，前端用于动态生成菜单导航")
    private List<SysMenu> routes = new ArrayList<>();

    /**
     * 系统默认首页路径
     *
     * <p>用户登录后默认跳转的首页路由路径。
     * 通常对应角色配置的defaultHomePageId对应的菜单路由路径。
     * 如果未配置，前端可使用第一个有权限的菜单作为首页。
     *
     * <p>路径格式：
     * <ul>
     *   <li>必须以"/"开头</li>
     *   <li>示例："/dashboard"、"/system/user"</li>
     *   <li>如果为空，表示使用系统默认首页</li>
     * </ul>
     */
    @ApiModelProperty(
            value = "系统默认首页路径",
            required = false,
            example = "/dashboard",
            notes = "用户登录后默认跳转的首页路由路径，通常对应角色配置的defaultHomePageId。如果为空，使用系统默认首页")
    private String home;
}
