package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.constant.MenuConstants;
import cn.pzhdv.blog.constant.RedisKey;
import cn.pzhdv.blog.entity.SysMenu;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.SysMenuService;
import cn.pzhdv.blog.utils.CacheExpireUtil;
import cn.pzhdv.blog.utils.Md5Util;
import cn.pzhdv.blog.utils.RedisUtils;
import cn.pzhdv.blog.vo.common.TreeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 系统菜单表 前端控制器
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02 18:04:32
 */
@Slf4j
@Validated
@Api(tags = "系统菜单管理")
@RestController
@RequestMapping("/systemManage/sysMenu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;
    private final RedisUtils redisUtils;


    @ApiLog("分页查询菜单树结构列表")
    @ApiOperation(value = "查询菜单列表并构建树结构（分页）", notes = "支持菜单名称、菜单类型、菜单状态等条件查询，返回分页的树形结构菜单列表", produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuName", value = "菜单名称（模糊匹配）", paramType = "query", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "menuType", value = "菜单类型（1=目录，2=菜单，精准匹配）", paramType = "query", dataType = "Byte", dataTypeClass = Byte.class),
            @ApiImplicitParam(name = "status", value = "菜单状态（1=启用，2=禁用，精准匹配）", paramType = "query", dataType = "Byte", dataTypeClass = Byte.class),
            @ApiImplicitParam(name = "parentId", value = "根节点ID", paramType = "query", dataType = "Long", dataTypeClass = Long.class, defaultValue = "0"),
            @ApiImplicitParam(name = "current", value = "当前页码（≥1）", paramType = "query", required = true, dataType = "Integer", dataTypeClass = Integer.class, example = "1", defaultValue = "1"),
            @ApiImplicitParam(name = "size", value = "每页条数（≥1）", paramType = "query", required = true, dataType = "Integer", dataTypeClass = Integer.class, example = "10", defaultValue = "10")
    })
    @GetMapping("getMenuListTree")
    public Result<Page<SysMenu>> getMenuListTree(
            @RequestParam(value = "menuName", required = false) String menuName,
            @RequestParam(value = "menuType", required = false) Byte menuType,
            @RequestParam(value = "status", required = false) Byte status,
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId,
            @RequestParam(value = "current", defaultValue = "1") @Min(value = 1, message = "当前页码必须大于等于1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {

        // 如果未指定defaultParentId，使用根菜单ID
        if (parentId == null) {
            parentId = MenuConstants.ROOT_PARENT_ID;
        }

        // 生成缓存 key
        String cacheKey = RedisKey.SYS_MENU_PAGE_KEY + Md5Util.md5Of(menuName, menuType, status, parentId, current, size);

        // 尝试从缓存获取
        Page<SysMenu> cachedPage = redisUtils.get(cacheKey, new TypeReference<>() {
        });
        if (cachedPage != null) {
            log.debug("【菜单分页列表】命中缓存 | key: {}", cacheKey);
            return ResultUtil.ok(cachedPage);
        }

        // 分页查询菜单列表
        Page<SysMenu> menuPage = sysMenuService.getMenuListTreePage(menuName, menuType, status, parentId, current, size);

        // 写入缓存
        redisUtils.set(cacheKey, menuPage, CacheExpireUtil.getDefaultExpireSeconds());

        log.info("分页查询菜单树结构列表成功，条件：menuName={}, menuType={}, status={}, parentId={}, 当前页码={}, 每页条数={}, 总条数={}, 总页数={}",
                menuName, menuType, status, parentId, current, size, menuPage.getTotal(), menuPage.getPages());

        return ResultUtil.ok(menuPage);
    }

    @ApiLog("获取菜单详情")
    @ApiOperation(value = "根据ID获取菜单详细信息", notes = "根据菜单ID查询菜单详细信息", produces = "application/json")
    @ApiImplicitParams({@ApiImplicitParam(name = "menuId", value = "菜单ID（必须为正整数）", paramType = "query", dataType = "Long", required = true, example = "2", dataTypeClass = Long.class)})
    @GetMapping("getMenuDetailById")
    public Result<SysMenu> getMenuDetailById(@RequestParam(value = "menuId", required = true) @Min(value = 1, message = "菜单ID必须为正整数") Long menuId) {

        // 生成缓存 key
        String cacheKey = RedisKey.SYS_MENU_DETAIL_KEY + menuId;

        // 尝试从缓存获取
        SysMenu cachedMenu = redisUtils.get(cacheKey, SysMenu.class);
        if (cachedMenu != null) {
            log.debug("【获取菜单详情】命中缓存 | 菜单ID: {}", menuId);
            return ResultUtil.ok(cachedMenu);
        }

        // 查询菜单信息
        SysMenu menu = sysMenuService.getById(menuId);
        if (menu == null) {
            log.warn("【获取菜单信息】失败 | 菜单不存在 | 菜单ID: {}", menuId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "菜单不存在");
        }

        // 写入缓存
        redisUtils.set(cacheKey, menu, CacheExpireUtil.getDefaultExpireSeconds());

        log.info("【获取菜单信息】成功 | 菜单ID: {}, 菜单名称: {}", menuId, menu.getMenuName());
        return ResultUtil.ok(menu);
    }

    @ApiLog("新增菜单")
    @ApiOperation(value = "新增菜单", notes = "新增系统菜单", produces = "application/json")
    @ApiOperationSupport(ignoreParameters = {"menuId", "children", "createTime", "updateTime", "deleted"})
    @PostMapping("add")
    public Result<Boolean> addMenu(@RequestBody @Validated SysMenu menu) {
        Long parentId = menu.getParentId();

        // 校验父级菜单是否存在
        if (!MenuConstants.ROOT_PARENT_ID.equals(parentId)) {
            SysMenu parentMenu = sysMenuService.getById(parentId);
            if (parentMenu == null) {
                log.warn("【新增菜单】失败 | 父级菜单不存在 | 父级菜单ID: {}, 菜单名称: {}", parentId, menu.getMenuName());
                return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "父级菜单不存在");
            }
        }

        // 自动生成 component 字段
        String component = generateComponent(menu.getMenuType(), menu.getRouteName().trim());
        menu.setComponent(component);
        //生成布局字段
        String layout = generateLayout(menu.getMenuType(), parentId, menu.getLayout());
        menu.setLayout(layout);

        // 保存菜单
        boolean success = sysMenuService.save(menu);
        if (!success) {
            log.error("【新增菜单】失败 | 数据库保存失败 | 菜单名称: {}", menu.getMenuName());
            return ResultUtil.error(ResultCode.ADD_FAIL, "新增菜单失败，请稍后重试");
        }

        log.info("【新增菜单】成功 | 菜单名称: {}, 菜单ID: {}, 父级菜单ID: {}, 菜单类型: {}", menu.getMenuName(), menu.getMenuId(), parentId, menu.getMenuType());

        // 清除缓存
        redisUtils.deleteKeysByPattern(RedisKey.SYS_MENU_PAGE_KEY + "*");
        redisUtils.del(RedisKey.SYS_MENU_TREE_KEY);
        redisUtils.del(RedisKey.SYS_MENU_ALL_PAGES_KEY);

        return ResultUtil.ok(true);
    }

    @ApiLog("编辑菜单")
    @ApiOperation(value = "编辑菜单", notes = "更新系统菜单信息（menuId为必填，用于定位待修改菜单；createdTime不允许修改，updatedTime由系统自动填充，children字段会被忽略）", produces = "application/json")
    @ApiOperationSupport(ignoreParameters = {"children", "createTime", "updateTime", "deleted"})
    @PutMapping("edit")
    public Result<Boolean> editMenu(@RequestBody @Validated SysMenu menu) {

        // 1. 校验菜单ID是否存在
        Long menuId = menu.getMenuId();
        if (menuId == null) {
            log.warn("【编辑菜单】失败 | 菜单ID不能为空");
            return ResultUtil.error(ResultCode.PARAM_REQUIRED, "菜单ID不能为空");
        }

        SysMenu existMenu = sysMenuService.getById(menuId);
        if (existMenu == null) {
            log.warn("【编辑菜单】失败 | 菜单不存在 | 菜单ID: {}", menuId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "菜单不存在，无法编辑");
        }

        // 3. 校验不能将自身作为父级菜单
        Long parentId = menu.getParentId();
        if (Objects.equals(parentId, menuId)) {
            log.warn("【编辑菜单】失败 | 不能将自身作为父级菜单 | 菜单ID: {}", menuId);
            return ResultUtil.error(ResultCode.PARAM_INVALID, "不能将自身作为父级菜单");
        }

        // 4. 校验父级菜单是否存在
        if (!MenuConstants.ROOT_PARENT_ID.equals(parentId)) {
            SysMenu parentMenu = sysMenuService.getById(parentId);
            if (parentMenu == null) {
                log.warn("【编辑菜单】失败 | 父级菜单不存在 | 菜单ID: {}, 父级菜单ID: {}, 菜单名称: {}", menuId, parentId, menu.getMenuName());
                return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "父级菜单不存在");
            }
        }

        // 5. 自动生成 component 字段
        String component = generateComponent(menu.getMenuType(), menu.getRouteName().trim());
        menu.setComponent(component);
        //生成布局字段
        String layout = generateLayout(menu.getMenuType(), parentId, menu.getLayout());
        menu.setLayout(layout);

        // 如果不是隐藏菜单 高亮的菜单设置为null
        if (!menu.getHideInMenu()) {
            menu.setActiveMenu(null);
        }

        // 6. 执行更新
        boolean success = sysMenuService.updateById(menu);
        if (!success) {
            log.error("【编辑菜单】失败 | 数据库更新失败 | 菜单ID: {}, 菜单名称: {}", menuId, menu.getMenuName());
            return ResultUtil.error(ResultCode.UPDATE_FAIL, "编辑菜单失败，请稍后重试");
        }

        log.info("【编辑菜单】成功 | 菜单ID: {}, 原菜单名称: {}, 新菜单名称: {}, 父级菜单ID: {}", menuId, existMenu.getMenuName(), menu.getMenuName(), parentId);

        // 清除缓存
        redisUtils.del(RedisKey.SYS_MENU_DETAIL_KEY + menuId);
        redisUtils.deleteKeysByPattern(RedisKey.SYS_MENU_PAGE_KEY + "*");
        redisUtils.del(RedisKey.SYS_MENU_TREE_KEY);
        redisUtils.del(RedisKey.SYS_MENU_ALL_PAGES_KEY);

        return ResultUtil.ok(true);

    }

    @ApiLog("删除菜单")
    @ApiOperation(value = "删除菜单", notes = "根据菜单ID删除系统菜单（谨慎操作，删除后子菜单将无法访问）", produces = "application/json")
    @DeleteMapping("delete/{menuId}")
    public Result<Boolean> deleteMenu(@PathVariable @ApiParam(name = "menuId", value = "菜单ID（≥1）", required = true) @Min(value = 1, message = "菜单ID必须为正整数") Long menuId) {

        // 1. 校验菜单是否存在
        SysMenu menu = sysMenuService.getById(menuId);
        if (menu == null) {
            log.warn("【删除菜单】失败 | 菜单不存在 | 菜单ID: {}", menuId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "菜单不存在，无法删除");
        }

        // 2. 校验是否存在子菜单
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysMenu::getParentId, menuId);
        long childCount = sysMenuService.count(queryWrapper);
        if (childCount > 0) {
            log.warn("【删除菜单】失败 | 存在子菜单，无法删除 | 菜单ID: {}, 菜单名称: {}, 子菜单数量: {}", menuId, menu.getMenuName(), childCount);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "存在子菜单，无法删除，请先删除子菜单");
        }

        // 3. 执行删除（逻辑删除）
        boolean success = sysMenuService.removeById(menuId);
        if (!success) {
            log.error("【删除菜单】失败 | 数据库删除失败 | 菜单ID: {}, 菜单名称: {}", menuId, menu.getMenuName());
            return ResultUtil.error(ResultCode.DELETE_FAIL, "数据库操作异常，删除失败，请稍后重试");
        }

        log.info("【删除菜单】成功 | 菜单ID: {}, 菜单名称: {}", menuId, menu.getMenuName());

        // 清除缓存
        redisUtils.del(RedisKey.SYS_MENU_DETAIL_KEY + menuId);
        redisUtils.deleteKeysByPattern(RedisKey.SYS_MENU_PAGE_KEY + "*");
        redisUtils.del(RedisKey.SYS_MENU_TREE_KEY);
        redisUtils.del(RedisKey.SYS_MENU_ALL_PAGES_KEY);

        return ResultUtil.ok(true);
    }

    @ApiLog("获取菜单树形结构")
    @ApiOperation(
            value = "获取菜单树形结构",
            notes = "获取整个菜单树形结构",
            produces = "application/json"
    )
    @GetMapping("getMenuTree")
    public Result<List<TreeVO>> getMenuTree() {

        // 尝试从缓存获取
        List<TreeVO> cachedTree = redisUtils.get(RedisKey.SYS_MENU_TREE_KEY, new TypeReference<>() {
        });
        if (cachedTree != null) {
            log.debug("【获取菜单树形结构】命中缓存 | key: {}", RedisKey.SYS_MENU_TREE_KEY);
            return ResultUtil.ok(cachedTree);
        }

        List<TreeVO> menuTree = sysMenuService.getAllMenuTree(MenuConstants.ROOT_PARENT_ID);

        // 写入缓存
        redisUtils.set(RedisKey.SYS_MENU_TREE_KEY, menuTree, CacheExpireUtil.getDefaultExpireSeconds());

        log.info("【获取菜单树形结构】成功 | 返回根节点数量: {}", menuTree.size());
        return ResultUtil.ok(menuTree);
    }

    @ApiLog("获取所有页面菜单")
    @ApiOperation(
            value = "获取所有页面菜单",
            notes = "获取所有启用的页面菜单（菜单类型=2），返回平铺的菜单列表，按排序字段和创建时间排序。适用于页面选择器、页面权限配置等场景。",
            produces = "application/json"
    )
    @GetMapping("getAllPages")
    public Result<List<SysMenu>> getAllPages() {

        // 尝试从缓存获取
        List<SysMenu> cachedPages = redisUtils.get(RedisKey.SYS_MENU_ALL_PAGES_KEY, new TypeReference<>() {
        });
        if (cachedPages != null) {
            log.debug("【获取所有页面菜单】命中缓存 | key: {}", RedisKey.SYS_MENU_ALL_PAGES_KEY);
            return ResultUtil.ok(cachedPages);
        }

        List<SysMenu> pages = sysMenuService.getAllPages(MenuConstants.MENU_TYPE_MENU);

        // 写入缓存
        redisUtils.set(RedisKey.SYS_MENU_ALL_PAGES_KEY, pages, CacheExpireUtil.getDefaultExpireSeconds());

        log.info("【获取所有页面菜单】成功 | 返回页面数量: {}", pages.size());
        return ResultUtil.ok(pages);
    }

    /**
     * 生成布局字段：仅目录类型/顶级菜单返回布局值，其他返回null
     * 规则：只有目录类型（menuType=1）或顶级菜单（parentId=0）需要选择布局
     *
     * @param menuType 菜单类型：1=目录，2=菜单
     * @param parentId 父级菜单ID，0表示根级
     * @param layout   传入的布局值（如base）
     * @return 布局值（目录/顶级菜单返回layout，其他返回null；layout为空时兜底空字符串）
     */
    private String generateLayout(Byte menuType, Long parentId, String layout) {
        boolean isRoot = MenuConstants.ROOT_PARENT_ID.equals(parentId);
        boolean isDirectory = MenuConstants.MENU_TYPE_DIRECTORY.equals(menuType);
        if (isDirectory || isRoot) {
            return layout;
        }
        return null;
    }

    /**
     * 根据菜单类型和路由名称自动生成 component 字段
     * <p>
     * 生成规则（核心区分文档路由、异常路由、普通路由三类场景）：
     * <pre>
     * 一、文档路由（路由名称以document前缀开头）
     *   1. 菜单类型=目录（1）：返回 null
     *   2. 菜单类型=菜单（2）：返回固定值 page.iframe-page
     *   3. 菜单类型=其他：返回空字符串，记录警告日志
     * 二、异常路由（路由名称以exception前缀开头）
     *   1. 菜单类型=目录（1）：返回 null
     *   2. 菜单类型=菜单（2）：提取路由名称下划线后状态码，拼接为 page.${状态码}；
     *      若无下划线/下划线在末尾，返回空字符串并记录错误日志
     *   3. 菜单类型=其他：返回空字符串，记录警告日志
     * 三、普通路由（非文档/非异常路由）
     *   1. 无论菜单类型是目录（1）还是菜单（2），均返回 page.(base)_ + 完整路由名称（保留特殊字符）
     * </pre>
     * 规则示例：
     * <pre>
     * 文档路由：
     *   menuType=1、routeName=document → null
     *   menuType=2、routeName=document_ui → page.iframe-page
     * 异常路由：
     *   menuType=1、routeName=exception → null
     *   menuType=2、routeName=exception_403 → page.403
     *   menuType=2、routeName=exception_ → 空字符串（日志报错）
     * 普通路由：
     *   menuType=1、routeName=home → page.(base)_home
     *   menuType=2、routeName=manage_role-[...slug] → page.(base)_manage_role-[...slug]
     * </pre>
     *
     * @param menuType  菜单类型：1=目录，2=菜单
     * @param routeName 路由名称（非空，文档路由以document开头，异常路由以exception开头）
     * @return 生成的 component 值（null/空字符串/指定格式字符串）
     */
    private String generateComponent(Byte menuType, String routeName) {
        // 1. 处理文档路由
        if (routeName.startsWith(MenuConstants.ROUTE_PREFIX_DOCUMENT)) {
            if (MenuConstants.MENU_TYPE_DIRECTORY.equals(menuType)) {
                return null; // 文档目录层 → null
            } else if (MenuConstants.MENU_TYPE_MENU.equals(menuType)) {
                return MenuConstants.COMPONENT_IFRAME_PAGE; // 文档菜单层 → 固定值
            } else {
                log.warn("文档路由匹配未知菜单类型 | menuType={}, routeName={}", menuType, routeName);
                return "";
            }
        }
        // 2. 处理异常路由
        if (routeName.startsWith(MenuConstants.ROUTE_PREFIX_EXCEPTION)) {
            if (MenuConstants.MENU_TYPE_DIRECTORY.equals(menuType)) {
                return null; // 异常目录层 → null
            } else if (MenuConstants.MENU_TYPE_MENU.equals(menuType)) {
                // 提取状态码（下划线后部分）
                int underscoreIndex = routeName.indexOf("_");
                if (underscoreIndex == -1 || underscoreIndex == routeName.length() - 1) {
                    log.error("异常路由未提取到状态码 | routeName={}", routeName);
                    return "";
                }
                String statusCode = routeName.substring(underscoreIndex + 1);
                return "page." + statusCode; // 异常菜单层 → page.${状态码}
            } else {
                log.warn("异常路由匹配未知菜单类型 | menuType={}, routeName={}", menuType, routeName);
                return "";
            }
        }

        // 3. 处理普通路由（非文档/非异常）
        return MenuConstants.COMPONENT_NORMAL_PREFIX + routeName; // 所有层级统一格式
    }
}