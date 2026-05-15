package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.dto.auth.LoginDTO;
import cn.pzhdv.blog.dto.auth.RefreshTokenDTO;
import cn.pzhdv.blog.entity.SysButton;
import cn.pzhdv.blog.entity.SysMenu;
import cn.pzhdv.blog.entity.SystemUser;
import cn.pzhdv.blog.vo.auth.LoginUserInfoVo;
import cn.pzhdv.blog.vo.auth.SystemMenuRoute;
import cn.pzhdv.blog.vo.token.TokenRefreshVO;

import cn.pzhdv.blog.vo.auth.SystemUserAuthTokenVO;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.entity.SysRole;
import cn.pzhdv.blog.entity.SysUserRole;
import cn.pzhdv.blog.service.SysButtonService;
import cn.pzhdv.blog.service.SysMenuService;
import cn.pzhdv.blog.service.SysRoleButtonService;
import cn.pzhdv.blog.service.SysRoleMenuService;
import cn.pzhdv.blog.service.SysRoleService;
import cn.pzhdv.blog.service.SystemUserService;
import cn.pzhdv.blog.service.SysUserRoleService;
import cn.pzhdv.blog.utils.JwtTokenUtil;
import cn.pzhdv.blog.utils.PasswordUtil;
import cn.pzhdv.blog.constant.StatusConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证授权控制器 负责用户登录、获取用户信息、刷新令牌等认证相关功能
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Slf4j
@Validated
@Api(tags = "认证授权管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenUtil jwtTokenUtil;
    private final SystemUserService baseService;
    private final SysUserRoleService sysUserRoleService;
    private final SysRoleService sysRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysRoleButtonService sysRoleButtonService;
    private final SysMenuService sysMenuService;
    private final SysButtonService sysButtonService;

    // ==================== 公共方法 ====================

    /**
     * 从请求中提取并解析用户ID
     *
     * @param request HTTP请求
     * @return 用户ID，解析失败返回null
     */
    private Long extractUserId(HttpServletRequest request) {
        String accessToken = jwtTokenUtil.extractTokenFromRequest(request);
        String userIdStr = jwtTokenUtil.getUserIdFromAccessToken(accessToken);
        try {
            return Long.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            log.error("用户ID格式错误 | userIdStr: {}", userIdStr, e);
            return null;
        }
    }

    /**
     * 获取用户关联的有效角色列表（状态正常的角色）
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    private List<SysRole> getUserValidRoles(Long userId) {
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoleList = sysUserRoleService.list(userRoleWrapper);

        if (CollectionUtils.isEmpty(userRoleList)) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoleList.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysRole::getRoleId, roleIds);
        roleWrapper.eq(SysRole::getStatus, StatusConstants.ROLE_STATUS_NORMAL);
        return sysRoleService.list(roleWrapper);
    }

    /**
     * 从角色列表中提取角色ID列表
     */
    private List<Long> extractRoleIds(List<SysRole> roles) {
        return roles.stream()
                .map(SysRole::getRoleId)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户按钮权限列表
     */
    private List<String> getButtonAuthList(List<Long> roleIds) {
        List<String> buttonAuthList = new ArrayList<>();
        if (roleIds.isEmpty()) {
            return buttonAuthList;
        }

        // 优化：一次查询所有角色的按钮关联
        List<Long> allButtonIds = sysRoleButtonService.getButtonIdsByRoleIds(roleIds);

        if (!allButtonIds.isEmpty()) {
            LambdaQueryWrapper<SysButton> buttonWrapper = new LambdaQueryWrapper<>();
            buttonWrapper.in(SysButton::getButtonId, allButtonIds);
            buttonWrapper.eq(SysButton::getStatus, StatusConstants.BUTTON_STATUS_ENABLED);
            List<SysButton> buttons = sysButtonService.list(buttonWrapper);
            buttonAuthList = buttons.stream()
                    .map(SysButton::getButtonCode)
                    .collect(Collectors.toList());
        }

        return buttonAuthList;
    }

    /**
     * 获取用户默认首页路径
     */
    private String getHomePath(List<SysRole> validRoles) {
        // 1. 收集所有角色的默认首页ID
        List<Long> homePageIds = validRoles.stream()
                .filter(role -> role.getDefaultHomePageId() != null && role.getDefaultHomePageId() > 0)
                .map(SysRole::getDefaultHomePageId)
                .distinct()
                .collect(Collectors.toList());

        if (homePageIds.isEmpty()) {
            return null;
        }

        // 2. 一次批量查询所有首页菜单
        List<SysMenu> homeMenus = sysMenuService.listByIds(homePageIds);

        // 3. 返回第一个有效的首页路径
        for (SysMenu menu : homeMenus) {
            if (menu != null && StringUtils.hasText(menu.getRoutePath())) {
                return menu.getRoutePath();
            }
        }
        return null;
    }

    // ==================== 业务接口 ====================

    @ApiLog("用户登录认证")
    @ApiOperation(
            value = "用户登录认证",
            notes = "系统用户登录验证，支持用户名密码登录，返回访问令牌和刷新令牌，用于后续API调用认证",
            produces = "application/json")
    @PostMapping("login")
    public Result<SystemUserAuthTokenVO> login(
            @ApiParam(value = "用户登录信息（用户名+密码）", required = true)
            @RequestBody
            @Validated
            LoginDTO loginDTO) {

        String userName = loginDTO.getUserName();
        String password = loginDTO.getPassword();

        // 1. 查询用户（用户名已通过@NotBlank校验，无需重复判断）
        SystemUser findSystemUser = baseService.findSystemUserByUserName(userName);
        if (findSystemUser == null) {
            log.warn("登录失败 | 用户名不存在 | 用户名: {}", userName);
            return ResultUtil.error(ResultCode.USER_NOT_EXIST, userName);
        }

        // 先检查账号是否被锁定（不透露密码是否正确）
        if (Objects.equals(findSystemUser.getStatus(), StatusConstants.USER_STATUS_FORBIDDEN)) {
            log.warn("登录失败 | 账号已被锁定 | 用户名: {}", userName);
            return ResultUtil.error(ResultCode.ACCOUNT_LOCKED);
        }

        // 2. 密码验证（加密校验，异常返回服务器错误）
        boolean isPasswordValid;
        try {
            isPasswordValid = PasswordUtil.verifyPassword(password, findSystemUser.getPassword());
        } catch (Exception e) {
            log.error("登录失败 | 密码校验异常 | 用户名: {}", userName, e);
            return ResultUtil.error(ResultCode.SERVER_ERROR, "密码校验异常，请重试");
        }
        if (!isPasswordValid) {
            log.warn("登录失败 | 密码错误 | 用户名: {}", userName);
            return ResultUtil.error(ResultCode.LOGIN_FAILED);
        }

        // 3. 生成Token（异常返回服务器错误）
        String accessToken;
        String refreshToken;
        Integer accessTokenExpire;
        try {
            // 登录时只查一次库，把 userId + username 都放进 Token
            String userId = String.valueOf(findSystemUser.getUserId());
            String username = findSystemUser.getUserName(); // 从数据库查出来的用户名

            // 生成双令牌
            accessToken = jwtTokenUtil.generateAccessToken(userId, username);
            refreshToken = jwtTokenUtil.generateRefreshToken(userId, username);

            // 获取过期时间
            accessTokenExpire = jwtTokenUtil.getAccessTokenExpireSeconds();
        } catch (Exception e) {
            log.error("登录失败 | Token生成异常 | 用户名: {}", userName, e);
            return ResultUtil.error(ResultCode.SERVER_ERROR, "令牌生成失败，请重试");
        }

        // 4. 构建返回结果
        SystemUserAuthTokenVO response = new SystemUserAuthTokenVO(accessToken, refreshToken);
        log.info(
                "登录成功 | 用户名: {}, 用户ID: {}, 令牌过期时间: {}秒",
                userName,
                findSystemUser.getUserId(),
                accessTokenExpire);
        return ResultUtil.ok(response);
    }

    @ApiLog("获取当前用户信息及权限")
    @ApiOperation(
            value = "获取当前用户信息及权限",
            notes = "获取当前登录的用户信息，包含角色,按钮权限（需要JWT Token验证）",
            produces = "application/json")
    @GetMapping("getLoginUserInfo")
    public Result<LoginUserInfoVo> getLoginUserInfo(HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            if (userId == null) {
                return ResultUtil.error(ResultCode.TOKEN_INVALID, "用户身份信息异常");
            }

            SystemUser user = baseService.getById(userId);
            if (user == null) {
                log.warn("获取用户信息失败 | 用户不存在 | 用户ID: {}", userId);
                return ResultUtil.error(ResultCode.USER_NOT_EXIST);
            }

            List<SysRole> validRoles = getUserValidRoles(userId);

            List<String> roleList = validRoles.stream()
                    .map(SysRole::getRoleCode)
                    .collect(Collectors.toList());

            List<Long> roleIds = extractRoleIds(validRoles);

            List<String> buttonAuthList = getButtonAuthList(roleIds);

            String homePath = getHomePath(validRoles);

            // 一次查询所有角色的菜单ID
            List<Long> allMenuIds = sysRoleMenuService.getMenuIdsByRoleIds(roleIds);
            boolean hasRoutePermission = !CollectionUtils.isEmpty(allMenuIds);

            LoginUserInfoVo loginUserInfoVo = new LoginUserInfoVo();
            loginUserInfoVo.setUserId(user.getUserId());
            loginUserInfoVo.setUserNick(user.getUserNick());
            loginUserInfoVo.setUserName(user.getUserName());
            loginUserInfoVo.setAvatar(user.getAvatar());
            loginUserInfoVo.setRoles(roleList);
            loginUserInfoVo.setButtons(buttonAuthList);
            loginUserInfoVo.setHomePath(homePath);
            loginUserInfoVo.setHasRoutePermission(hasRoutePermission);

            log.info("获取用户信息成功 | 用户ID: {}, 用户名: {}, 角色数量: {}, 按钮权限数量: {}, 默认首页: {}, 路由权限: {}",
                    userId, user.getUserName(), roleList.size(), buttonAuthList.size(), homePath, hasRoutePermission);
            return ResultUtil.ok(loginUserInfoVo);

        } catch (Exception e) {
            log.error("获取用户信息异常", e);
            return ResultUtil.error(ResultCode.SERVER_ERROR, "获取用户信息失败，请稍后重试");
        }
    }

    @ApiLog("刷新访问令牌和刷新令牌")
    @ApiOperation(
            value = "刷新访问令牌和刷新令牌",
            notes = "使用有效的刷新令牌获取新的访问令牌和刷新令牌，提高安全性并延长用户会话有效期",
            produces = "application/json")
    @PostMapping("/refreshToken")
    public Result<TokenRefreshVO> refreshToken(
            @ApiParam(value = "刷新令牌请求", required = true) @RequestBody @Validated
            RefreshTokenDTO refreshTokenDTO) {

        try {
            String refreshToken = refreshTokenDTO.getRefreshToken();

            // 1. 参数校验
            if (!StringUtils.hasText(refreshToken)) {
                log.warn("刷新令牌失败 | 刷新令牌为空");
                return ResultUtil.error(ResultCode.PARAM_INVALID, "刷新令牌不能为空");
            }

            // 2. 验证刷新令牌有效性
            if (!jwtTokenUtil.validateRefreshToken(refreshToken)) {
                log.warn("刷新令牌失败 | 刷新令牌无效或已过期");
                return ResultUtil.error(ResultCode.REFRESH_TOKEN_EXPIRED);
            }

            // 3. 从刷新令牌中获取用户ID
            String userId = jwtTokenUtil.getUserIdFromRefreshToken(refreshToken);

            // 4. 生成新的访问令牌和刷新令牌
            String newAccessToken = jwtTokenUtil.generateAccessToken(userId);
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(userId);

            if (!StringUtils.hasText(newAccessToken) || !StringUtils.hasText(newRefreshToken)) {
                log.error("刷新令牌失败 | 生成新令牌失败 | 用户ID: {}", userId);
                return ResultUtil.error(ResultCode.SERVER_ERROR, "令牌刷新失败，请重试");
            }

            // 5. 构建返回结果（包含新的访问令牌和刷新令牌）
            TokenRefreshVO response = new TokenRefreshVO(newAccessToken, newRefreshToken);

            // 6. 记录日志（过期时间仅用于日志记录，不影响业务逻辑）
            Integer accessTokenExpire = jwtTokenUtil.getAccessTokenExpireSeconds();
            if (accessTokenExpire != null && accessTokenExpire > 0) {
                log.info("刷新令牌成功 | 用户ID: {}, 新访问令牌过期时间: {}秒", userId, accessTokenExpire);
            } else {
                log.warn("刷新令牌成功 | 用户ID: {}, 但访问令牌过期时间配置异常，请检查配置", userId);
            }

            return ResultUtil.ok(response);

        } catch (Exception e) {
            log.error("刷新令牌异常", e);
            return ResultUtil.error(ResultCode.SERVER_ERROR, "令牌刷新失败，请稍后重试");
        }
    }


    @ApiLog("获取系统菜单路由信息")
    @ApiOperation(
            value = "获取系统菜单路由信息",
            notes = "根据当前登录用户的角色权限，获取用户可访问的菜单路由树形结构和默认首页路径。需要JWT Token验证。",
            produces = "application/json")
    @GetMapping("getSystemRoutes")
    public Result<SystemMenuRoute> getSystemRoutes(HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            if (userId == null) {
                return ResultUtil.error(ResultCode.TOKEN_INVALID, "用户身份信息异常");
            }

            List<SysRole> validRoles = getUserValidRoles(userId);

            if (validRoles.isEmpty()) {
                log.warn("获取菜单路由失败 | 用户没有有效角色 | 用户ID: {}", userId);
                SystemMenuRoute systemMenuRoute = new SystemMenuRoute();
                systemMenuRoute.setRoutes(new ArrayList<>());
                systemMenuRoute.setHome(null);
                return ResultUtil.ok(systemMenuRoute);
            }

            List<Long> roleIds = extractRoleIds(validRoles);

            // 一次查询所有角色的菜单ID
            List<Long> allMenuIds = sysRoleMenuService.getMenuIdsByRoleIds(roleIds);

            List<SysMenu> menuRoutes;
            if (allMenuIds.isEmpty()) {
                log.warn("获取菜单路由失败 | 角色未分配菜单权限 | 用户ID: {}", userId);
                menuRoutes = new ArrayList<>();
            } else {
                menuRoutes = sysMenuService.getMenuRoutesByIds(allMenuIds);
                log.info("获取菜单路由成功 | 用户ID: {}, 菜单数量: {}", userId, menuRoutes.size());
            }

            String homePath = getHomePath(validRoles);
            if (homePath != null) {
                log.info("获取默认首页路径成功 | 用户ID: {}, 首页路径: {}", userId, homePath);
            }

            SystemMenuRoute systemMenuRoute = new SystemMenuRoute();
            systemMenuRoute.setRoutes(menuRoutes);
            systemMenuRoute.setHome(homePath);

            log.info("获取系统菜单路由成功 | 用户ID: {}, 菜单数量: {}, 默认首页: {}", userId, menuRoutes.size(), homePath);
            return ResultUtil.ok(systemMenuRoute);

        } catch (Exception e) {
            log.error("获取系统菜单路由异常", e);
            return ResultUtil.error(ResultCode.SERVER_ERROR, "获取菜单路由失败，请稍后重试");
        }
    }

}
