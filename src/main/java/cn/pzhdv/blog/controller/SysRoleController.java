package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.constant.RedisKey;
import cn.pzhdv.blog.dto.common.BatchDeleteReq;
import cn.pzhdv.blog.dto.system.role.SysRoleAddDTO;
import cn.pzhdv.blog.dto.system.role.SysRoleEditDTO;
import cn.pzhdv.blog.entity.SysRole;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.SysRoleButtonService;
import cn.pzhdv.blog.service.SysRoleMenuService;
import cn.pzhdv.blog.service.SysRoleService;
import cn.pzhdv.blog.utils.CacheExpireUtil;
import cn.pzhdv.blog.utils.Md5Util;
import cn.pzhdv.blog.utils.RedisUtils;
import cn.pzhdv.blog.vo.role.SysRoleSimpleVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 系统角色管理控制器
 * <p>
 * 提供角色的分页查询、新增、编辑、单条删除、批量删除等核心操作，
 * 所有接口均包含参数校验、业务规则校验、异常处理，保证接口稳定性和安全性。
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-01 18:53:32
 */
@Slf4j
@Validated
@Api(tags = "系统角色管理")
@RestController
@RequestMapping("/systemManage/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysRoleButtonService sysRoleButtonService;
    private final RedisUtils redisUtils;

    /**
     * 角色列表条件分页查询
     *
     * @param roleName 角色名称（模糊匹配，可为空）
     * @param roleCode 角色编码（模糊匹配，可为空）
     * @param status   角色状态（1:正常 2:禁止，精准匹配，可为空）
     * @param current  当前页码（≥1）
     * @param size     每页条数（≥1，无上限）
     * @return 分页结果（包含角色列表、总条数、总页数）
     */
    @ApiLog("分页查询角色列表")
    @ApiOperation(value = "角色列表条件分页查询", notes = "支持角色名称、角色编码模糊匹配，角色状态精准匹配；默认分页10条/页，页码≥1，每页条数≥1（无上限）", produces = "application/json")
    @ApiImplicitParams({@ApiImplicitParam(name = "roleName", value = "角色名称（模糊匹配）", paramType = "query", dataType = "String", dataTypeClass = String.class, example = "超级管理员"), @ApiImplicitParam(name = "roleCode", value = "角色编码（模糊匹配）", paramType = "query", dataType = "String", dataTypeClass = String.class, example = "admin"), @ApiImplicitParam(name = "status", value = "角色状态(1:正常 2:禁止，精准匹配）", paramType = "query", dataType = "Byte", dataTypeClass = Byte.class, example = "1"), @ApiImplicitParam(name = "current", value = "当前页码（≥1）", paramType = "query", required = true, dataType = "Integer", dataTypeClass = Integer.class, example = "1", defaultValue = "1"), @ApiImplicitParam(name = "size", value = "每页条数（≥1，无上限）", paramType = "query", required = true, dataType = "Integer", dataTypeClass = Integer.class, example = "10", defaultValue = "10")})
    @GetMapping("/getRoleList")
    public Result<Page<SysRole>> getRoleList(@RequestParam(value = "roleName", required = false) String roleName, @RequestParam(value = "roleCode", required = false) String roleCode, @RequestParam(value = "status", required = false) Byte status, @RequestParam(value = "current", defaultValue = "1") @Min(value = 1, message = "当前页码必须大于等于1") Integer current, @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {

        // 生成缓存 key
        String cacheKey = RedisKey.SYS_ROLE_PAGE_KEY + buildRoleListCacheKey(roleName, roleCode, status, current, size);

        // 尝试从缓存获取
        Page<SysRole> cachedPage = redisUtils.get(cacheKey, new TypeReference<>() {
        });
        if (cachedPage != null) {
            log.debug("【角色列表】命中缓存 | key: {}", cacheKey);
            return ResultUtil.ok(cachedPage);
        }

        // 分页查询
        Page<SysRole> rolePage = sysRoleService.queryRoleListByConditionPage(roleName, roleCode, status, current, size);

        // 写入缓存
        redisUtils.set(cacheKey, rolePage, CacheExpireUtil.getDefaultExpireSeconds());

        // 日志打印
        log.info("分页查询系统角色列表成功，条件：roleName={}, roleCode={}, status={}, 当前页码={}, 每页条数={}, 总条数={}, 总页数={}", roleName, roleCode, status, current, size, rolePage.getTotal(), rolePage.getPages());

        return ResultUtil.ok(rolePage);
    }

    /**
     * 构建角色列表缓存 key（对查询参数进行 MD5 哈希压缩）
     */
    private String buildRoleListCacheKey(String roleName, String roleCode, Byte status, Integer current, Integer size) {
        return Md5Util.md5Of(roleName, roleCode, status, current, size);
    }

    @ApiLog("查询所有角色（精简版）")
    @ApiOperation(value = "角色列表查询", notes = "仅返回角色的roleId、roleCode、roleName字段", produces = "application/json")
    @GetMapping("/getAllRoles")
    public Result<List<SysRoleSimpleVO>> getAllRoles() {
        // 尝试从缓存获取
        List<SysRoleSimpleVO> cachedList = redisUtils.get(RedisKey.SYS_ROLE_ALL_KEY, new TypeReference<>() {
        });
        if (cachedList != null) {
            log.debug("【角色列表】命中缓存 | key: {}", RedisKey.SYS_ROLE_ALL_KEY);
            return ResultUtil.ok(cachedList);
        }

        // 1. 查询所有角色数据
        List<SysRole> sysRoleList = sysRoleService.list();

        // 2. 仅保留roleId/roleCode/roleName字段（通过VO封装，避免返回多余字段）
        List<SysRoleSimpleVO> resultList = sysRoleList.stream().map(role -> {
            SysRoleSimpleVO simpleVO = new SysRoleSimpleVO();
            simpleVO.setRoleId(role.getRoleId());
            simpleVO.setRoleCode(role.getRoleCode());
            simpleVO.setRoleName(role.getRoleName());
            return simpleVO;
        }).toList();

        // 写入缓存
        redisUtils.set(RedisKey.SYS_ROLE_ALL_KEY, resultList, CacheExpireUtil.getDefaultExpireSeconds());

        // 3. 日志打印
        log.info("查询角色列表成功，仅返回roleId/roleCode/roleName字段，总条数：{}", resultList.size());

        // 4. 返回结果
        return ResultUtil.ok(resultList);
    }

    /**
     * 新增系统角色
     *
     * @param addDTO 新增角色请求参数（包含编码、名称、状态等核心字段）
     * @return 新增结果（true:成功 false:失败）
     */
    @ApiLog("新增系统角色")
    @ApiOperation(value = "新增系统角色", notes = "角色编码需全局唯一；创建/更新时间由系统自动填充，无需前端传递；排序权重默认传0，数值越小越靠前", produces = "application/json")
    @PostMapping("/add")
    public Result<Boolean> addRole(@RequestBody @Valid SysRoleAddDTO addDTO) {
        try {
            // 1. 参数预处理：去除首尾空格
            String roleName = addDTO.getRoleName().trim();
            String roleCode = addDTO.getRoleCode().trim();
            addDTO.setRoleName(roleName);
            addDTO.setRoleCode(roleCode);

            // 2. 校验角色编码唯一性
            if (sysRoleService.checkRoleCodeIsExist(roleCode)) {
                log.warn("新增角色失败 | 角色编码已存在 | 编码：{}", roleCode);
                return ResultUtil.error(ResultCode.DUPLICATE_DATA, "角色编码[" + roleCode + "]已被占用");
            }

            // 4. DTO转实体
            SysRole sysRole = new SysRole();
            BeanUtils.copyProperties(addDTO, sysRole);

            // 5. 保存角色
            boolean success = sysRoleService.save(sysRole);
            if (success) {
                log.info("新增角色成功 | 角色名称：{}，角色编码：{}，自动生成创建时间：{}", roleName, roleCode, sysRole.getCreateTime());

                // 清除缓存
                redisUtils.deleteKeysByPattern(RedisKey.SYS_ROLE_PAGE_KEY + "*");
                redisUtils.del(RedisKey.SYS_ROLE_ALL_KEY);
                redisUtils.del(RedisKey.SYS_ROLE_CODE_EXISTS_KEY + roleCode);

                return ResultUtil.ok(true);
            }

            log.error("新增角色失败 | 数据库保存失败 | 角色编码：{}", roleCode);
            return ResultUtil.error(ResultCode.ADD_FAIL, "新增角色失败，请稍后重试");
        } catch (Exception e) {
            log.error("新增角色异常 | 角色名称：{}，角色编码：{}", addDTO.getRoleName(), addDTO.getRoleCode(), e);
            return ResultUtil.error(ResultCode.SERVER_ERROR, "服务器异常，请联系管理员");
        }
    }

    /**
     * 编辑系统角色
     *
     * @param editDTO 编辑角色请求参数（包含角色ID、新名称/状态/编码等字段）
     * @return 编辑结果（true:成功 false:失败）
     */
    @ApiLog("编辑系统角色")
    @ApiOperation(value = "编辑系统角色", notes = "1. 角色ID为必填，用于定位待修改角色；2. 角色编码若修改需保证全局唯一；3. 更新时间由系统自动填充，无需传递；4. 创建时间不允许修改", produces = "application/json")
    @PutMapping("/edit")
    public Result<Boolean> editRole(@RequestBody @Valid SysRoleEditDTO editDTO) {

        // 1. 参数预处理：去除首尾空格
        String roleName = editDTO.getRoleName();
        String roleCode = editDTO.getRoleCode();

        // 2. 校验角色是否存在
        Long roleId = editDTO.getRoleId();
        SysRole existRole = sysRoleService.getById(roleId);
        if (existRole == null) {
            log.warn("编辑角色失败 | 角色不存在 | 角色ID：{}", roleId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "角色不存在，无法编辑");
        }

        // 3. 校验角色编码唯一性（仅当编码修改时校验）
        if (!existRole.getRoleCode().equals(roleCode)) {
            if (sysRoleService.checkRoleCodeIsExist(roleCode)) {
                log.warn("编辑角色失败 | 角色编码已存在 | 角色ID：{}，新编码：{}", roleId, roleCode);
                return ResultUtil.error(ResultCode.DUPLICATE_DATA, "角色编码[" + roleCode + "]已被占用");
            }
        }

        // 5. DTO转实体
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(editDTO, sysRole);
        sysRole.setCreateTime(existRole.getCreateTime()); // 强制保留创建时间
        sysRole.setUpdateTime(null); // 清空更新时间，触发自动填充

        // 6. 执行更新
        boolean success = sysRoleService.updateById(sysRole);
        if (!success) {
            log.error("编辑角色失败 | 数据库更新失败 | 角色ID：{}，角色编码：{}", roleId, roleCode);
            return ResultUtil.error(ResultCode.UPDATE_FAIL, "编辑角色失败，请稍后重试");
        }
        log.info("编辑角色成功 | 角色ID：{}，原名称：{}，新名称：{}，原编码：{}，新编码：{}，自动生成更新时间：{}", roleId, existRole.getRoleName(), roleName, existRole.getRoleCode(), roleCode, sysRole.getUpdateTime());

        // 清除缓存
        redisUtils.deleteKeysByPattern(RedisKey.SYS_ROLE_PAGE_KEY + "*");
        redisUtils.del(RedisKey.SYS_ROLE_ALL_KEY);
        redisUtils.del(RedisKey.SYS_ROLE_CODE_EXISTS_KEY + existRole.getRoleCode());
        redisUtils.del(RedisKey.SYS_ROLE_CODE_EXISTS_KEY + roleCode);

        return ResultUtil.ok(true);
    }

    /**
     * 根据角色 ID删除系统角色
     *
     * @param roleId 角色ID（≥1）
     * @return 删除结果（true:成功 false:失败）
     */
    @ApiLog("删除系统角色")
    @ApiOperation(value = "删除系统角色", notes = "根据角色ID删除系统角色（谨慎操作，删除后关联权限自动失效）。删除角色时会自动删除角色菜单中间表中的关联数据。", produces = "application/json")
    @DeleteMapping("delete/{roleId}")
    public Result<Boolean> deleteById(@PathVariable @ApiParam(name = "roleId", value = "系统角色ID（≥1）", required = true) @Min(value = 1, message = "角色 ID必须为正整数") Long roleId) {

        // 1. 校验角色是否存在
        SysRole role = sysRoleService.getById(roleId);
        if (role == null) {
            log.warn("删除失败 | 角色不存在 | 角色ID: {}", roleId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "角色不存在，无法删除");
        }

        // 2. 删除角色菜单中间表中的关联数据
        boolean menuDeleteSuccess = sysRoleMenuService.deleteByRoleId(roleId);
        if (!menuDeleteSuccess) {
            log.error("【删除系统角色】操作失败 | 原因：删除角色菜单关联失败 | 角色ID：{} | 角色名称：{} | 角色编码：{}", roleId, role.getRoleName(), role.getRoleCode());
            return ResultUtil.error(ResultCode.DELETE_FAIL, "删除角色菜单关联失败，请稍后重试");
        }

        // 3. 删除角色按钮中间表中的关联数据
        boolean buttonDeleteSuccess = sysRoleButtonService.deleteByRoleId(roleId);
        if (!buttonDeleteSuccess) {
            log.error("【删除系统角色】操作失败 | 原因：删除角色按钮关联失败 | 角色ID：{} | 角色名称：{} | 角色编码：{}", roleId, role.getRoleName(), role.getRoleCode());
            return ResultUtil.error(ResultCode.DELETE_FAIL, "删除角色按钮关联失败，请稍后重试");
        }

        // 4. 执行删除角色
        boolean success = sysRoleService.removeById(roleId);
        if (!success) {
            log.error("【删除系统角色】操作失败 | 原因：数据库删除失败 | 角色ID：{} | 角色名称：{} | 角色编码：{}", roleId, role.getRoleName(), role.getRoleCode());
            return ResultUtil.error(ResultCode.DELETE_FAIL, "数据库操作异常，删除失败，请稍后重试");
        }

        log.info("【删除系统角色】操作成功 | 角色ID：{} | 角色名称：{} | 角色编码：{}", roleId, role.getRoleName(), role.getRoleCode());

        // 清除缓存
        redisUtils.deleteKeysByPattern(RedisKey.SYS_ROLE_PAGE_KEY + "*");
        redisUtils.del(RedisKey.SYS_ROLE_ALL_KEY);
        redisUtils.del(RedisKey.SYS_ROLE_CODE_EXISTS_KEY + role.getRoleCode());

        return ResultUtil.ok(true);
    }

    /**
     * 批量删除系统角色
     *
     * @param deleteReq 批量删除请求参数（包含角色ID列表）
     * @return 删除结果（true:成功 false:失败）
     */
    @ApiLog("批量删除系统角色")
    @ApiOperation(value = "批量删除系统角色", notes = "批量删除系统角色（谨慎操作，ID≥1）；若部分ID不存在，会返回不存在的ID列表，不执行删除。删除角色时会自动删除角色菜单中间表中的关联数据。", produces = "application/json")
    @DeleteMapping("delete/batch")
    public Result<Boolean> deleteBatch(@RequestBody @Valid BatchDeleteReq deleteReq) {

        // 1. 获取角色ID列表
        List<Long> roleIdList = deleteReq.getIds();

        // 2. 空列表校验
        if (roleIdList.isEmpty()) {
            log.warn("批量删除失败 | 待删除角色ID列表为空");
            return ResultUtil.error(ResultCode.DELETE_FAIL, "待删除角色 ID列表不能为空");
        }

        // 3. 批量校验角色是否存在
        List<SysRole> existRoles = sysRoleService.listByIds(roleIdList);
        List<Long> existRoleIdList = existRoles.stream().map(SysRole::getRoleId).toList();

        // 4. 筛选不存在的角色ID
        List<Long> notExistIds = roleIdList.stream().filter(id -> !existRoleIdList.contains(id)).toList();

        // 5. 存在不存在的ID则返回提示
        if (!notExistIds.isEmpty()) {
            log.warn("批量删除失败 | 部分角色ID不存在 | 不存在的ID列表：{}", notExistIds);
            return ResultUtil.error(ResultCode.DELETE_FAIL, String.format("以下ID对应的角色不存在：%s", notExistIds));
        }

        // 6. 删除角色菜单中间表中的关联数据
        boolean menuDeleteSuccess = sysRoleMenuService.deleteByRoleIds(roleIdList);
        if (!menuDeleteSuccess) {
            log.error("【批量删除角色】删除失败 | 删除角色菜单关联失败 | 待删除角色数量：{} | 角色ID列表：{}", roleIdList.size(), roleIdList);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "删除角色菜单关联失败，请稍后重试");
        }

        // 7. 删除角色按钮中间表中的关联数据
        boolean buttonDeleteSuccess = sysRoleButtonService.deleteByRoleIds(roleIdList);
        if (!buttonDeleteSuccess) {
            log.error("【批量删除角色】删除失败 | 删除角色按钮关联失败 | 待删除角色数量：{} | 角色ID列表：{}", roleIdList.size(), roleIdList);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "删除角色按钮关联失败，请稍后重试");
        }

        // 8. 执行批量删除角色
        boolean success = sysRoleService.removeByIds(roleIdList);
        if (!success) {
            log.error("【批量删除角色】删除失败 | 数据库操作失败 | 待删除角色数量：{} | 角色ID列表：{}", roleIdList.size(), roleIdList);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "数据库操作异常，请稍后重试");
        }

        // 9. 拼接删除成功的角色名称
        List<String> deletedRoleNames = existRoles.stream().map(SysRole::getRoleName).toList();
        log.info("【批量删除角色】删除成功 | 已删除角色数量：{} | 角色ID列表：{} | 角色名称：{}", roleIdList.size(), roleIdList, deletedRoleNames);

        // 清除缓存
        redisUtils.deleteKeysByPattern(RedisKey.SYS_ROLE_PAGE_KEY + "*");
        redisUtils.del(RedisKey.SYS_ROLE_ALL_KEY);
        existRoles.forEach(role -> redisUtils.del(RedisKey.SYS_ROLE_CODE_EXISTS_KEY + role.getRoleCode()));

        return ResultUtil.ok(true);
    }


    @ApiLog("校验角色编码唯一性")
    @ApiOperation(value = "校验角色编码唯一性", notes = "新增/编辑角色时校验角色编码是否已存在，返回true表示已存在，false表示不存在", produces = "application/json")
    @ApiImplicitParams({@ApiImplicitParam(name = "roleCode", value = "角色编码", paramType = "query", dataType = "String", required = true, dataTypeClass = String.class)})
    @GetMapping("checkRoleCodeExists")
    public Result<Boolean> checkRoleCodeExists(@ApiParam(value = "角色编码", required = true, example = "ADMIN_001") @RequestParam(value = "roleCode") @NotBlank(message = "角色编码不能为空") String roleCode) {
        // 尝试从缓存获取
        String cacheKey = RedisKey.SYS_ROLE_CODE_EXISTS_KEY + roleCode;
        Boolean cached = redisUtils.get(cacheKey, Boolean.class);
        if (cached != null) {
            log.debug("【检查角色编码】命中缓存 | 角色编码: {}, 存在: {}", roleCode, cached);
            return ResultUtil.ok(cached);
        }

        // 调用服务层校验角色编码是否存在
        boolean isExist = sysRoleService.checkRoleCodeIsExist(roleCode);

        // 写入缓存
        redisUtils.set(cacheKey, isExist, CacheExpireUtil.getDefaultExpireSeconds());

        // 日志记录
        log.info("角色编码{} | 角色编码: {}", isExist ? "已存在" : "不存在", roleCode);

        return ResultUtil.ok(isExist);
    }
}