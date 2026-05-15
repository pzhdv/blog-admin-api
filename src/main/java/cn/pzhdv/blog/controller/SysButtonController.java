package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.constant.RedisKey;
import cn.pzhdv.blog.dto.common.BatchDeleteReq;
import cn.pzhdv.blog.dto.system.button.SysButtonAddDTO;
import cn.pzhdv.blog.dto.system.button.SysButtonEditDTO;
import cn.pzhdv.blog.entity.SysMenu;
import cn.pzhdv.blog.entity.SysButton;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.SysButtonService;
import cn.pzhdv.blog.service.SysMenuService;
import cn.pzhdv.blog.utils.CacheExpireUtil;
import cn.pzhdv.blog.utils.Md5Util;
import cn.pzhdv.blog.utils.RedisUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统按钮权限管理控制器
 * <p>
 * 提供按钮权限的完整CRUD操作，包括：分页查询、新增、编辑、单条删除、批量删除、编码唯一性校验等。
 * </p>
 * <p>
 * 业务规则说明：
 * <ul>
 *   <li>按钮编码全局唯一，用于权限标识，格式如 user:add、system:menu:add（字母开头，必须包含冒号）</li>
 *   <li>按钮与关联，删除时需注意级联影响</li>
 *   <li>支持逻辑删除，删除后数据可恢复</li>
 *   <li>创建/更新时间由系统自动管理，前端无需传递</li>
 * </ul>
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-05
 */
@Slf4j
@Validated
@Api(tags = "系统按钮权限管理")
@RestController
@RequestMapping("/systemManage/SysButton")
@RequiredArgsConstructor
public class SysButtonController {

    /**
     * 按钮编码正则表达式
     * <p>规则：字母开头，支持字母/数字/冒号/下划线，必须包含至少一个冒号</p>
     */
    private static final String BUTTON_CODE_REGEX = "^[a-zA-Z][a-zA-Z0-9:_]*:[a-zA-Z0-9:_]+$";

    private final SysButtonService sysButtonService;
    private final SysMenuService sysMenuService;
    private final RedisUtils redisUtils;


    @ApiLog("查询按钮权限列表")
    @ApiOperation(value = "按钮权限列表查询", notes = "支持ID、按钮状态条件查询，返回完整列表（不分页）", produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuId", value = "所属ID（精准匹配）", paramType = "query", dataType = "Long", dataTypeClass = Long.class, example = "1"),
            @ApiImplicitParam(name = "status", value = "按钮状态（1:正常 2:禁用，精准匹配）", paramType = "query", dataType = "Byte", dataTypeClass = Byte.class, example = "1"),
    })
    @GetMapping("/getSysButtonList")
    public Result<List<SysButton>> getSysButtonList(
            @RequestParam(value = "menuId", required = false) Long menuId,
            @RequestParam(value = "status", required = false) Byte status) {

        // 生成缓存 key
        String cacheKey = RedisKey.SYS_BUTTON_LIST_KEY + buildButtonListCacheKey(menuId, status);

        // 尝试从缓存获取
        List<SysButton> cachedList = redisUtils.get(cacheKey, new TypeReference<>() {
        });
        if (cachedList != null) {
            log.debug("【查询按钮权限列表】命中缓存 | key: {}", cacheKey);
            return ResultUtil.ok(cachedList);
        }

        List<SysButton> buttonList = sysButtonService.querySysButtonListByCondition(menuId, status);

        // 写入缓存
        redisUtils.set(cacheKey, buttonList, CacheExpireUtil.getDefaultExpireSeconds());

        log.info("【查询按钮权限列表】成功 | 条件: menuId={}, status={}, 总条数={}", menuId, status, buttonList.size());
        return ResultUtil.ok(buttonList);
    }

    /**
     * 构建按钮列表缓存 key（对查询参数进行 MD5 哈希压缩）
     */
    private String buildButtonListCacheKey(Long menuId, Byte status) {
        return Md5Util.md5Of(menuId, status);
    }


    @ApiLog("分页查询按钮权限列表")
    @ApiOperation(value = "按钮权限列表条件分页查询", notes = "支持ID、按钮状态等条件分页查询；默认分页10条/页，页码≥1，每页条数≥1", produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuId", value = "所属ID（精准匹配）", paramType = "query", dataType = "Long", dataTypeClass = Long.class, example = "1"),
            @ApiImplicitParam(name = "status", value = "按钮状态（1:正常 2:禁用，精准匹配）", paramType = "query", dataType = "Byte", dataTypeClass = Byte.class, example = "1"),
            @ApiImplicitParam(name = "current", value = "当前页码（≥1）", paramType = "query", required = true, dataType = "Integer", dataTypeClass = Integer.class, example = "1", defaultValue = "1"),
            @ApiImplicitParam(name = "size", value = "每页条数（≥1）", paramType = "query", required = true, dataType = "Integer", dataTypeClass = Integer.class, example = "10", defaultValue = "10")
    })
    @GetMapping("/getSysButtonListByConditionPage")
    public Result<Page<SysButton>> querySysButtonListByConditionPage(
            @RequestParam(value = "menuId", required = false) Long menuId,
            @RequestParam(value = "status", required = false) Byte status,
            @RequestParam(value = "current", defaultValue = "1") @Min(value = 1, message = "当前页码必须大于等于1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {

        // 生成缓存 key
        String cacheKey = RedisKey.SYS_BUTTON_PAGE_KEY + buildButtonPageCacheKey(menuId, status, current, size);

        // 尝试从缓存获取
        Page<SysButton> cachedPage = redisUtils.get(cacheKey, new TypeReference<>() {});
        if (cachedPage != null) {
            log.debug("【分页查询按钮权限列表】命中缓存 | key: {}", cacheKey);
            return ResultUtil.ok(cachedPage);
        }

        Page<SysButton> buttonPage = sysButtonService.querySysButtonListByConditionPage(menuId, status, current, size);

        // 写入缓存
        redisUtils.set(cacheKey, buttonPage, CacheExpireUtil.getDefaultExpireSeconds());

        log.info("【分页查询按钮权限列表】成功 | 条件: menuId={}, status={}, 当前页码={}, 每页条数={}, 总条数={}, 总页数={}",
                menuId, status, current, size, buttonPage.getTotal(), buttonPage.getPages());
        return ResultUtil.ok(buttonPage);
    }

    /**
     * 构建按钮分页缓存 key（对查询参数进行 MD5 哈希压缩）
     */
    private String buildButtonPageCacheKey(Long menuId, Byte status, Integer current, Integer size) {
        return Md5Util.md5Of(menuId, status, current, size);
    }


    @ApiLog("获取按钮权限详情")
    @ApiOperation(value = "根据ID获取按钮权限详情", notes = "根据按钮ID查询按钮权限详细信息，同时返回关联的名称", produces = "application/json")
    @ApiImplicitParams({@ApiImplicitParam(name = "buttonId", value = "按钮ID（必须为正整数）", paramType = "query", dataType = "Long", required = true, example = "1", dataTypeClass = Long.class)})
    @GetMapping("getSysButtonDetailById")
    public Result<SysButton> getSysButtonDetailById(
            @RequestParam(value = "buttonId") @Min(value = 1, message = "按钮ID必须为正整数") Long buttonId) {

        // 尝试从缓存获取
        String cacheKey = RedisKey.SYS_BUTTON_DETAIL_KEY + buttonId;
        SysButton cachedButton = redisUtils.get(cacheKey, SysButton.class);
        if (cachedButton != null) {
            log.debug("【获取按钮权限详情】命中缓存 | 按钮ID: {}", buttonId);
            return ResultUtil.ok(cachedButton);
        }

        SysButton button = sysButtonService.getById(buttonId);
        if (button == null) {
            log.warn("【获取按钮权限详情】失败 | 按钮不存在 | 按钮ID: {}", buttonId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "按钮权限不存在");
        }

        SysMenu menu = sysMenuService.getById(button.getMenuId());
        if (menu != null) {
            button.setMenuName(menu.getMenuName());
        }

        // 写入缓存
        redisUtils.set(cacheKey, button, CacheExpireUtil.getDefaultExpireSeconds());

        log.info("【获取按钮权限详情】成功 | 按钮ID: {}, 按钮名称: {}", buttonId, button.getButtonName());
        return ResultUtil.ok(button);
    }


    @ApiLog("新增按钮权限")
    @ApiOperation(value = "新增按钮权限", notes = "按钮编码需全局唯一；创建/更新时间由系统自动填充，无需前端传递", produces = "application/json")
    @PostMapping("/add")
    public Result<Boolean> addSysButton(@RequestBody @Valid SysButtonAddDTO addDTO) {
        try {
            String buttonName = addDTO.getButtonName().trim();
            String buttonCode = addDTO.getButtonCode().trim();
            addDTO.setButtonName(buttonName);
            addDTO.setButtonCode(buttonCode);

            SysMenu menu = sysMenuService.getById(addDTO.getMenuId());
            if (menu == null) {
                log.warn("【新增按钮权限】失败 | 不存在 | ID: {}", addDTO.getMenuId());
                return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "不存在");
            }

            if (sysButtonService.checkButtonCodeExists(buttonCode)) {
                log.warn("【新增按钮权限】失败 | 按钮编码已存在 | 按钮编码: {}", buttonCode);
                return ResultUtil.error(ResultCode.DUPLICATE_DATA, "按钮编码[" + buttonCode + "]已被占用");
            }

            SysButton SysButton = new SysButton();
            BeanUtils.copyProperties(addDTO, SysButton);

            boolean success = sysButtonService.save(SysButton);
            if (success) {
                log.info("【新增按钮权限】成功 | 按钮名称: {}, 按钮编码: {}, 所属: {}, 创建时间: {}",
                        buttonName, buttonCode, menu.getMenuName(), SysButton.getCreateTime());

                // 清除缓存
                redisUtils.deleteKeysByPattern(RedisKey.SYS_BUTTON_PAGE_KEY + "*");
                redisUtils.deleteKeysByPattern(RedisKey.SYS_BUTTON_LIST_KEY + "*");
                redisUtils.del(RedisKey.SYS_BUTTON_CODE_EXISTS_KEY + buttonCode);

                return ResultUtil.ok(true);
            }

            log.error("【新增按钮权限】失败 | 数据库保存失败 | 按钮编码: {}", buttonCode);
            return ResultUtil.error(ResultCode.ADD_FAIL, "新增按钮权限失败，请稍后重试");
        } catch (Exception e) {
            log.error("【新增按钮权限】异常 | 按钮名称: {}, 按钮编码: {}", addDTO.getButtonName(), addDTO.getButtonCode(), e);
            return ResultUtil.error(ResultCode.SERVER_ERROR, "服务器异常，请联系管理员");
        }
    }


    @ApiLog("编辑按钮权限")
    @ApiOperation(value = "编辑按钮权限", notes = "1. 按钮ID为必填，用于定位待修改按钮；2. 按钮编码若修改需保证全局唯一；3. 创建时间不允许修改；4. 更新时间由系统自动填充", produces = "application/json")
    @PutMapping("/edit")
    public Result<Boolean> editSysButton(@RequestBody @Valid SysButtonEditDTO editDTO) {
        try {
            String buttonName = editDTO.getButtonName().trim();
            String buttonCode = editDTO.getButtonCode().trim();
            editDTO.setButtonName(buttonName);
            editDTO.setButtonCode(buttonCode);

            Long buttonId = editDTO.getButtonId();
            SysButton existButton = sysButtonService.getById(buttonId);
            if (existButton == null) {
                log.warn("【编辑按钮权限】失败 | 按钮不存在 | 按钮ID: {}", buttonId);
                return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "按钮权限不存在，无法编辑");
            }

            if (!existButton.getButtonCode().equals(buttonCode)) {
                if (sysButtonService.checkButtonCodeExists(buttonCode)) {
                    log.warn("【编辑按钮权限】失败 | 按钮编码已存在 | 按钮ID: {}, 新编码: {}", buttonId, buttonCode);
                    return ResultUtil.error(ResultCode.DUPLICATE_DATA, "按钮编码[" + buttonCode + "]已被占用");
                }
            }

            SysMenu menu = sysMenuService.getById(editDTO.getMenuId());
            if (menu == null) {
                log.warn("【编辑按钮权限】失败 | 不存在 | ID: {}", editDTO.getMenuId());
                return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "不存在");
            }

            SysButton SysButton = new SysButton();
            BeanUtils.copyProperties(editDTO, SysButton);
            SysButton.setCreateTime(existButton.getCreateTime());
            SysButton.setUpdateTime(null);

            boolean success = sysButtonService.updateById(SysButton);
            if (!success) {
                log.error("【编辑按钮权限】失败 | 数据库更新失败 | 按钮ID: {}, 按钮编码: {}", buttonId, buttonCode);
                return ResultUtil.error(ResultCode.UPDATE_FAIL, "编辑按钮权限失败，请稍后重试");
            }

            log.info("【编辑按钮权限】成功 | 按钮ID: {}, 原名称: {}, 新名称: {}, 原编码: {}, 新编码: {}, 更新时间: {}",
                    buttonId, existButton.getButtonName(), buttonName, existButton.getButtonCode(), buttonCode, SysButton.getUpdateTime());

            // 清除缓存
            redisUtils.del(RedisKey.SYS_BUTTON_DETAIL_KEY + buttonId);
            redisUtils.deleteKeysByPattern(RedisKey.SYS_BUTTON_PAGE_KEY + "*");
            redisUtils.deleteKeysByPattern(RedisKey.SYS_BUTTON_LIST_KEY + "*");
            redisUtils.del(RedisKey.SYS_BUTTON_CODE_EXISTS_KEY + existButton.getButtonCode());
            redisUtils.del(RedisKey.SYS_BUTTON_CODE_EXISTS_KEY + buttonCode);

            return ResultUtil.ok(true);
        } catch (Exception e) {
            log.error("【编辑按钮权限】异常 | 按钮ID: {}", editDTO.getButtonId(), e);
            return ResultUtil.error(ResultCode.SERVER_ERROR, "服务器异常，请联系管理员");
        }
    }


    @ApiLog("删除按钮权限")
    @ApiOperation(value = "删除按钮权限", notes = "根据按钮ID删除按钮权限（逻辑删除，删除后相关权限将失效）", produces = "application/json")
    @DeleteMapping("delete/{buttonId}")
    public Result<Boolean> deleteById(
            @PathVariable
            @ApiParam(name = "buttonId", value = "按钮ID（必须为正整数）", required = true)
            @Min(value = 1, message = "按钮ID必须为正整数") Long buttonId) {

        SysButton button = sysButtonService.getById(buttonId);
        if (button == null) {
            log.warn("【删除按钮权限】失败 | 按钮不存在 | 按钮ID: {}", buttonId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "按钮权限不存在，无法删除");
        }

        boolean success = sysButtonService.removeById(buttonId);
        if (!success) {
            log.error("【删除按钮权限】失败 | 数据库删除失败 | 按钮ID: {}, 按钮名称: {}, 按钮编码: {}",
                    buttonId, button.getButtonName(), button.getButtonCode());
            return ResultUtil.error(ResultCode.DELETE_FAIL, "数据库操作异常，删除失败，请稍后重试");
        }

        log.info("【删除按钮权限】成功 | 按钮ID: {}, 按钮名称: {}, 按钮编码: {}", buttonId, button.getButtonName(), button.getButtonCode());

        // 清除缓存
        redisUtils.del(RedisKey.SYS_BUTTON_DETAIL_KEY + buttonId);
        redisUtils.deleteKeysByPattern(RedisKey.SYS_BUTTON_PAGE_KEY + "*");
        redisUtils.deleteKeysByPattern(RedisKey.SYS_BUTTON_LIST_KEY + "*");
        redisUtils.del(RedisKey.SYS_BUTTON_CODE_EXISTS_KEY + button.getButtonCode());

        return ResultUtil.ok(true);
    }


    @ApiLog("批量删除按钮权限")
    @ApiOperation(value = "批量删除按钮权限", notes = "批量删除按钮权限（谨慎操作）；若部分ID不存在，会返回不存在的ID列表提示，不执行删除", produces = "application/json")
    @DeleteMapping("delete/batch")
    public Result<Boolean> deleteBatch(@RequestBody @Valid BatchDeleteReq deleteReq) {

        List<Long> buttonIdList = deleteReq.getIds();
        if (buttonIdList.isEmpty()) {
            log.warn("【批量删除按钮权限】失败 | 待删除按钮ID列表为空");
            return ResultUtil.error(ResultCode.DELETE_FAIL, "待删除按钮ID列表不能为空");
        }

        List<SysButton> existButtons = sysButtonService.listByIds(buttonIdList);
        Set<Long> existButtonIdSet = existButtons.stream()
                .map(SysButton::getButtonId)
                .collect(Collectors.toSet());

        List<Long> notExistIds = buttonIdList.stream()
                .filter(id -> !existButtonIdSet.contains(id))
                .collect(Collectors.toList());

        if (!notExistIds.isEmpty()) {
            log.warn("【批量删除按钮权限】失败 | 部分按钮ID不存在 | 不存在的ID列表: {}", notExistIds);
            return ResultUtil.error(ResultCode.DELETE_FAIL, String.format("以下ID对应的按钮权限不存在：%s", notExistIds));
        }

        boolean success = sysButtonService.removeByIds(buttonIdList);
        if (!success) {
            log.error("【批量删除按钮权限】失败 | 数据库操作失败 | 待删除按钮数量: {}, 按钮ID列表: {}", buttonIdList.size(), buttonIdList);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "数据库操作异常，请稍后重试");
        }

        List<String> deletedButtonNames = existButtons.stream()
                .map(SysButton::getButtonName)
                .collect(Collectors.toList());
        log.info("【批量删除按钮权限】成功 | 已删除按钮数量: {}, 按钮ID列表: {}, 按钮名称: {}",
                buttonIdList.size(), buttonIdList, deletedButtonNames);

        // 清除缓存
        buttonIdList.forEach(id -> redisUtils.del(RedisKey.SYS_BUTTON_DETAIL_KEY + id));
        redisUtils.deleteKeysByPattern(RedisKey.SYS_BUTTON_PAGE_KEY + "*");
        redisUtils.deleteKeysByPattern(RedisKey.SYS_BUTTON_LIST_KEY + "*");
        existButtons.forEach(b -> redisUtils.del(RedisKey.SYS_BUTTON_CODE_EXISTS_KEY + b.getButtonCode()));

        return ResultUtil.ok(true);
    }


    @ApiLog("校验按钮编码唯一性")
    @ApiOperation(value = "校验按钮编码唯一性", notes = "新增/编辑按钮时校验按钮编码是否已存在，返回true表示已存在，false表示不存在；会校验编码格式", produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "buttonCode", value = "按钮编码（如 user:add、system:menu:add）", paramType = "query", dataType = "String", required = true, dataTypeClass = String.class)
    })
    @GetMapping("checkButtonCodeExists")
    public Result<Boolean> checkButtonCodeExists(
            @ApiParam(value = "按钮编码", required = true, example = "user:add")
            @RequestParam(value = "buttonCode") String buttonCode) {

        if (buttonCode == null || buttonCode.trim().isEmpty()) {
            log.warn("【校验按钮编码唯一性】失败 | 按钮编码为空");
            return ResultUtil.error(ResultCode.PARAM_REQUIRED, "按钮编码不能为空");
        }

        String trimmedCode = buttonCode.trim();
        if (!trimmedCode.matches(BUTTON_CODE_REGEX)) {
            log.warn("【校验按钮编码唯一性】失败 | 按钮编码格式错误 | 按钮编码: {}", buttonCode);
            return ResultUtil.error(ResultCode.PARAM_INVALID, "按钮编码格式错误，如 user:add 或 system:menu:add");
        }

        // 尝试从缓存获取
        String cacheKey = RedisKey.SYS_BUTTON_CODE_EXISTS_KEY + trimmedCode;
        Boolean cached = redisUtils.get(cacheKey, Boolean.class);
        if (cached != null) {
            log.debug("【校验按钮编码唯一性】命中缓存 | 按钮编码: {}, 存在: {}", trimmedCode, cached);
            return ResultUtil.ok(cached);
        }

        boolean isExist = sysButtonService.checkButtonCodeExists(trimmedCode);

        // 写入缓存
        redisUtils.set(cacheKey, isExist, CacheExpireUtil.getDefaultExpireSeconds());

        if (isExist) {
            log.info("【校验按钮编码唯一性】按钮编码已存在 | 按钮编码: {}", buttonCode);
        } else {
            log.info("【校验按钮编码唯一性】按钮编码不存在 | 按钮编码: {}", buttonCode);
        }

        return ResultUtil.ok(isExist);
    }

}