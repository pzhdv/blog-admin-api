package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.SysRoleMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 角色菜单关联中间表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-04 23:44:17
 */
public interface SysRoleMenuService extends IService<SysRoleMenu> {

    /**
     * 根据角色ID获取菜单ID列表
     * <p>
     * 功能说明：
     * 1. 从角色菜单中间表查询指定角色拥有的所有菜单ID
     * 2. 返回菜单ID列表，用于后续获取菜单详情
     * </p>
     *
     * @param roleId 角色ID，不能为null
     * @return 菜单ID列表，如果角色没有关联菜单则返回空列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /**
     * 更新角色的菜单权限
     * <p>
     * 功能说明：
     * 1. 先删除该角色的所有旧菜单关联
     * 2. 批量插入新的菜单关联
     * 3. 使用事务保证数据一致性
     * </p>
     *
     * @param roleId 角色ID，不能为null
     * @param menuIds 菜单ID列表，可以为空（表示清空所有菜单权限）
     * @return 更新是否成功
     */
    boolean updateRoleMenus(Long roleId, List<Long> menuIds);

    /**
     * 根据角色ID删除角色菜单关联
     * <p>
     * 功能说明：
     * 1. 删除指定角色的所有菜单关联记录
     * 2. 用于角色删除时清理关联数据
     * </p>
     *
     * @param roleId 角色ID，不能为null
     * @return 删除是否成功
     */
    boolean deleteByRoleId(Long roleId);

    /**
     * 批量删除角色菜单关联
     * <p>
     * 功能说明：
     * 1. 批量删除指定角色ID列表的所有菜单关联记录
     * 2. 用于批量删除角色时清理关联数据
     * </p>
     *
     * @param roleIds 角色ID列表，不能为空
     * @return 删除是否成功
     */
    boolean deleteByRoleIds(List<Long> roleIds);

    /**
     * 根据多个角色ID批量获取菜单ID列表（优化N+1查询）
     * <p>
     * 功能说明：
     * 1. 使用IN语句一次性查询所有角色的菜单关联
     * 2. 返回所有角色关联的菜单ID集合
     * </p>
     *
     * @param roleIds 角色ID列表，不能为空
     * @return 所有角色关联的菜单ID集合
     */
    List<Long> getMenuIdsByRoleIds(List<Long> roleIds);
}
