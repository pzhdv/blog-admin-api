package cn.pzhdv.blog.vo.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录用户信息视图对象（VO）
 *
 * <p>用于接口返回登录用户的完整信息，包含用户基本信息、角色列表和按钮权限列表。
 * 屏蔽密码、手机号、邮箱等敏感字段，确保接口返回数据的安全性。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "LoginUserInfoVo", description = "登录用户信息视图对象，包含用户基本信息、角色和权限")
public class LoginUserInfoVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "登录用户ID", example = "1001", required = true)
    private Long userId;

    @ApiModelProperty(value = "用户昵称", example = "张小明", required = true)
    private String userNick;

    @ApiModelProperty(value = "用户名", example = "zhangxiaoming", required = true)
    private String userName;

    @ApiModelProperty(value = "用户头像URL", example = "https://xxx.com/avatar/1001.jpg", required = false)
    private String avatar;

    /**
     * 用户角色列表
     *
     * <p>用户拥有的角色标识列表，用于权限控制和菜单显示。
     * 角色标识通常以"R_"开头，如"R_SUPER"、"R_ADMIN"等。
     */
    @ApiModelProperty(
            value = "用户角色列表",
            required = true,
            example = "[\"R_SUPER\", \"R_ADMIN\"]",
            notes = "用户拥有的角色标识列表，用于权限控制")
    private List<String> roles = new ArrayList<>();

    /**
     * 按钮权限列表
     *
     * <p>用户拥有的按钮级权限列表，用于控制前端按钮的显示和隐藏。
     * 权限标识通常以"模块:操作"的格式，如"user:add"、"user:update"等。
     */
    @ApiModelProperty(
            value = "按钮权限列表",
            required = true,
            example = "[\"user:add\", \"user:update\", \"user:delete\", \"user:list\"]",
            notes = "用户拥有的按钮级权限列表，用于控制前端按钮显示")
    private List<String> buttons = new ArrayList<>();

    @ApiModelProperty(value = "默认首页路径", example = "/home", notes = "用户角色对应的默认首页路由路径")
    private String homePath;

    @ApiModelProperty(value = "用户是否有路由权限", example = "true", notes = "用户是否拥有菜单路由权限")
    private Boolean hasRoutePermission;
}