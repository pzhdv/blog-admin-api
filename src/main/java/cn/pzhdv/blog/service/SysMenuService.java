package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.SysMenu;
import cn.pzhdv.blog.vo.common.TreeVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


/**
 * <p>
 * 系统菜单表 服务类
 * </p>
 * <p>
 * 提供菜单的查询、树形结构构建等功能，支持分页查询、条件筛选、树形结构转换等操作。
 * 优化了N+1查询问题，使用内存构建树形结构，提升查询性能。
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02 18:04:32
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 分页查询菜单列表并构建树结构
     * <p>
     * 功能说明：
     * 1. 根据条件分页查询顶级菜单（parentId = rootParentId）
     * 2. 自动查询并包含所有层级的子菜单
     * 3. 在内存中构建完整的树形结构
     * 4. 优化了N+1查询问题，仅执行2次数据库查询
     * </p>
     *
     * @param menuName 菜单名称（模糊匹配，可为null）
     * @param menuType 菜单类型（1=目录，2=菜单，可为null）
     * @param status 菜单状态（1=启用，2=禁用，可为null）
     * @param rootParentId 根节点的父ID，通常为0L
     * @param current 当前页码（从1开始）
     * @param size 每页条数
     * @return 分页的树结构菜单列表，包含所有子菜单
     */
    Page<SysMenu> getMenuListTreePage(String menuName, Byte menuType, Byte status, Long rootParentId, Integer current, Integer size);

    /**
     * 根据菜单ID列表获取菜单路由信息（树形结构）
     * <p>
     * 功能说明：
     * 1. 根据菜单ID列表查询菜单信息（只查询启用的菜单）
     * 2. 自动向上查找并包含所有父级菜单（保证树形结构完整）
     * 3. 自动向下查找并包含所有子菜单（递归查找所有层级）
     * 4. 构建完整的树形结构，便于前端直接使用
     * </p>
     * <p>
     * 使用场景：适用于从角色菜单中间表查询出角色拥有的菜单ID列表后，获取对应的菜单路由
     * </p>
     *
     * @param menuIds 菜单ID列表（从角色菜单中间表查询得到），不能为空
     * @return 菜单路由信息列表（树形结构），包含所有父级和子级菜单
     */
    List<SysMenu> getMenuRoutesByIds(List<Long> menuIds);

    /**
     * 获取所有菜单的树形结构（转换为TreeVO格式）
     * <p>
     * 功能说明：
     * 1. 查询所有启用的菜单
     * 2. 根据rootParentId获取根节点菜单
     * 3. 构建完整的树形结构
     * 4. 转换为TreeVO格式，适配前端树形组件（ElementUI/AntD Tree）
     * </p>
     * <p>
     * TreeVO字段映射：
     * - value: 菜单ID
     * - label: 菜单名称
     * - i18nKey: 国际化key
     * - children: 子菜单列表
     * </p>
     *
     * @param rootParentId 根节点的父ID，通常为0L，不能为null
     * @return TreeVO格式的菜单树形结构列表
     */
    List<TreeVO> getAllMenuTree(Long rootParentId);

    /**
     * 根据菜单类型获取所有启用的菜单
     * <p>
     * 功能说明：
     * 1. 根据菜单类型查询所有启用的菜单
     * 2. 按排序字段和创建时间排序
     * 3. 返回平铺的菜单列表（非树形结构）
     * </p>
     * <p>
     * 使用场景：用于页面选择器、页面权限配置等场景，只需要获取所有可用的指定类型菜单
     * </p>
     *
     * @param menuType 菜单类型（1=目录，2=菜单），不能为null
     * @return 所有启用的指定类型菜单列表
     */
    List<SysMenu> getAllPages(Byte menuType);
}
