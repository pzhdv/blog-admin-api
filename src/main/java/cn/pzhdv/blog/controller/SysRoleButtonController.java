package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.entity.SysRole;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.SysRoleButtonService;
import cn.pzhdv.blog.service.SysRoleService;
import cn.pzhdv.blog.vo.role.RoleButtonVO;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;

/**
 * <p>
 * 角色-按钮权限关联表 前端控制器
 * </p>
 *
 * <p>
 * 功能说明：
 *  1. 提供角色与按钮权限的关联管理
 *  2. 根据角色ID查询已授权的按钮ID列表
 *  3. 更新角色的按钮权限（先删后增）
 *  4. 用于后台权限配置页面的按钮权限分配
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-06 13:39:50
 */
@Slf4j
@Validated
@Api(tags = "角色按钮权限管理")
@RestController
@RequestMapping("/sys-role-button")
@RequiredArgsConstructor
public class SysRoleButtonController {

    private final SysRoleService sysRoleService;
    private final SysRoleButtonService sysRoleButtonService;

    /**
     * 根据角色ID获取按钮列表
     * <p>
     * 业务逻辑：
     * 1. 校验角色是否存在
     * 2. 查询该角色已绑定的所有按钮ID
     * 3. 封装返回给前端用于权限回显
     * </p>
     *
     * @param roleId 角色ID（≥1）
     * @return 角色按钮权限VO（包含角色ID + 按钮ID列表）
     */
    @ApiLog("根据角色ID获取按钮权限列表")
    @ApiOperation(
            value = "根据角色ID获取按钮列表",
            notes = "根据角色ID从角色按钮中间表查询出角色拥有的按钮ID列表，用于权限配置页面回显。",
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
    @GetMapping("getButtonIdsByRoleId/{roleId}")
    public Result<RoleButtonVO> getButtonIdsByRoleId(
            @PathVariable
            @ApiParam(name = "roleId", value = "角色ID（≥1）", required = true)
            @Min(value = 1, message = "角色ID必须为正整数")
            Long roleId) {

        // 1. 校验角色是否存在
        SysRole role = sysRoleService.getById(roleId);
        if (role == null) {
            log.warn("【根据角色ID获取按钮列表】失败 | 角色不存在 | 角色ID: {}", roleId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }

        // 2. 从角色按钮中间表获取按钮ID列表
        RoleButtonVO roleButtonVO = new RoleButtonVO();
        roleButtonVO.setRoleId(role.getRoleId());
        roleButtonVO.setButtonIdList(sysRoleButtonService.getButtonIdsByRoleId(roleId));
        log.info("【根据角色ID获取按钮列表】成功 | 角色ID: {}", roleId);
        return ResultUtil.ok(roleButtonVO);
    }

    /**
     * 更新角色按钮权限
     * <p>
     * 业务逻辑：
     * 1. 校验角色ID是否为空、角色是否存在
     * 2. 删除该角色原有所有按钮权限关联
     * 3. 批量插入新的按钮权限关联
     * 4. 完成角色按钮权限的重新分配
     * </p>
     *
     * @param roleButtonVO 角色按钮权限VO（角色ID + 按钮ID列表）
     * @return 更新结果
     */
    @ApiLog("更新角色按钮权限")
    @ApiOperation(
            value = "更新角色按钮权限",
            notes = "更新角色的按钮权限。先删除该角色的所有旧按钮关联，再批量插入新的按钮关联。",
            produces = "application/json"
    )
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "roleButtonVO",
                    value = "角色按钮权限信息（包含角色ID、按钮ID列表）",
                    paramType = "body",
                    required = true,
                    dataType = "RoleButtonVO",
                    dataTypeClass = RoleButtonVO.class
            )
    })
    @PutMapping("updateRoleButtonAuth")
    public Result<Boolean> updateRoleButtonAuth(@RequestBody @Valid RoleButtonVO roleButtonVO) {

        // 1. 参数校验
        Long roleId = roleButtonVO.getRoleId();
        if (roleId == null) {
            log.warn("【更新角色按钮权限】失败 | 角色ID不能为空");
            return ResultUtil.error(ResultCode.PARAM_REQUIRED, "角色ID不能为空");
        }

        // 2. 校验角色是否存在
        SysRole role = sysRoleService.getById(roleId);
        if (role == null) {
            log.warn("【更新角色按钮权限】失败 | 角色不存在 | 角色ID: {}", roleId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }

        // 3. 更新角色按钮关联（先删后增）
        boolean updateSuccess = sysRoleButtonService.updateRoleButtons(roleId, roleButtonVO.getButtonIdList());
        if (!updateSuccess) {
            log.error("【更新角色按钮权限】失败 | 更新按钮关联失败 | 角色ID: {}", roleId);
            return ResultUtil.error(ResultCode.UPDATE_FAIL, "更新按钮关联失败，请稍后重试");
        }

        log.info("【更新角色按钮权限】成功 | 角色ID: {}", roleId);
        return ResultUtil.ok(true);
    }

}