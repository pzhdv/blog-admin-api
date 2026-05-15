package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.entity.SysRole;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.SysRoleMenuService;
import cn.pzhdv.blog.service.SysRoleService;
import cn.pzhdv.blog.vo.role.RoleMenuVO;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * <p>
 * 角色菜单关联中间表 前端控制器
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-06 13:59:12
 */
@Slf4j
@Validated
@Api(tags = "角色菜单权限管理")
@RestController
@RequestMapping("/sys-role-menu")
@RequiredArgsConstructor
public class SysRoleMenuController {

    private final SysRoleService sysRoleService;
    private final SysRoleMenuService sysRoleMenuService;

    /**
     * 根据角色ID获取菜单列表及首页菜单ID
     */
    @ApiLog("根据角色ID获取菜单权限列表")
    @ApiOperation(
            value = "根据角色ID获取菜单列表及首页菜单ID",
            notes = "根据角色ID从角色菜单中间表查询出角色拥有的菜单ID列表，同时返回首页菜单ID。",
            produces = "application/json"
    )
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "roleId",
                    value = "角色ID（≥1）",
                    paramType = "path",
                    dataType = "Long",
                    required = true,
                    dataTypeClass = Long.class,
                    example = "1"
            )
    })
    @GetMapping("getMenuIdsAndHomeByRoleId/{roleId}")
    public Result<RoleMenuVO> getMenuIdsAndHomeByRoleId(
            @PathVariable
            @ApiParam(name = "roleId", value = "角色ID（≥1）", required = true)
            @Min(value = 1, message = "角色ID必须为正整数")
            Long roleId) {

        // 1. 校验角色是否存在
        SysRole role = sysRoleService.getById(roleId);
        if (role == null) {
            log.warn("【根据角色ID获取菜单列表】失败 | 角色不存在 | 角色ID: {}", roleId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }

        // 2. 从角色菜单中间表获取菜单ID列表
        List<Long> menuIds = sysRoleMenuService.getMenuIdsByRoleId(roleId);
        log.info("【根据角色ID获取菜单列表】成功 | 角色ID: {}, 该角色未关联任何菜单", roleId);
        RoleMenuVO roleMenuVO = new RoleMenuVO();
        roleMenuVO.setMenuIdList(menuIds);
        roleMenuVO.setRoleId(role.getRoleId());
        roleMenuVO.setDefaultHomePageId(role.getDefaultHomePageId());
        return ResultUtil.ok(roleMenuVO);
    }

    /**
     * 更新角色菜单权限
     */
    @ApiLog("更新角色菜单权限")
    @ApiOperation(
            value = "更新角色菜单权限",
            notes = "更新角色的菜单权限和默认首页ID。先删除该角色的所有旧菜单关联，再批量插入新的菜单关联，同时更新角色的默认首页ID。",
            produces = "application/json"
    )
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "roleMenuVO",
                    value = "角色菜单权限信息（包含角色ID、菜单ID列表、默认首页ID）",
                    paramType = "body",
                    required = true,
                    dataType = "RoleMenuVO",
                    dataTypeClass = RoleMenuVO.class
            )
    })
    @PutMapping("updateRoleMenuAuth")
    public Result<Boolean> updateRoleMenuAuth(@RequestBody @Valid RoleMenuVO roleMenuVO) {

        // 1. 参数校验
        Long roleId = roleMenuVO.getRoleId();
        if (roleId == null) {
            log.warn("【更新角色菜单权限】失败 | 角色ID不能为空");
            return ResultUtil.error(ResultCode.PARAM_REQUIRED, "角色ID不能为空");
        }

        // 2. 校验角色是否存在
        SysRole role = sysRoleService.getById(roleId);
        if (role == null) {
            log.warn("【更新角色菜单权限】失败 | 角色不存在 | 角色ID: {}", roleId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }

        // 3. 更新角色菜单关联
        List<Long> menuIdList = roleMenuVO.getMenuIdList();
        boolean menuUpdateSuccess = sysRoleMenuService.updateRoleMenus(roleId, menuIdList);
        if (!menuUpdateSuccess) {
            log.error("【更新角色菜单权限】失败 | 更新菜单关联失败 | 角色ID: {}", roleId);
            return ResultUtil.error(ResultCode.UPDATE_FAIL, "更新菜单关联失败，请稍后重试");
        }

        // 4. 更新角色的默认首页ID
        Long defaultHomePageId = roleMenuVO.getDefaultHomePageId();
        role.setDefaultHomePageId(defaultHomePageId);
        boolean roleUpdateSuccess = sysRoleService.updateById(role);
        if (!roleUpdateSuccess) {
            log.error("【更新角色菜单权限】失败 | 更新角色默认首页ID失败 | 角色ID: {}", roleId);
            return ResultUtil.error(ResultCode.UPDATE_FAIL, "更新角色默认首页ID失败，请稍后重试");
        }

        log.info("【更新角色菜单权限】成功 | 角色ID: {}, 角色名称: {}, 菜单数量: {}, 默认首页ID: {}",
                roleId, role.getRoleName(), menuIdList != null ? menuIdList.size() : 0, defaultHomePageId);
        return ResultUtil.ok(true);
    }

}