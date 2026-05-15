package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.SysRoleButton;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 角色-按钮权限关联表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-06 13:39:50
 */
public interface SysRoleButtonService extends IService<SysRoleButton> {

    /**
     * 根据角色ID获取按钮ID列表
     *
     * @param roleId 角色ID，不能为null
     * @return 按钮ID列表，如果角色没有关联按钮则返回空列表
     */
    List<Long> getButtonIdsByRoleId(Long roleId);

    /**
     * 更新角色的按钮权限
     * <p>
     * 功能说明：
     * 1. 先删除该角色的所有旧按钮关联
     * 2. 批量插入新的按钮关联
     * 3. 使用事务保证数据一致性
     * </p>
     *
     * @param roleId 角色ID，不能为null
     * @param buttonIds 按钮ID列表，可以为空（表示清空所有按钮权限）
     * @return 更新是否成功
     */
    boolean updateRoleButtons(Long roleId, List<Long> buttonIds);

    /**
     * 根据角色ID删除角色按钮关联
     *
     * @param roleId 角色ID，不能为null
     * @return 删除是否成功
     */
    boolean deleteByRoleId(Long roleId);

    /**
     * 批量删除角色按钮关联
     *
     * @param roleIds 角色ID列表，不能为空
     * @return 删除是否成功
     */
    boolean deleteByRoleIds(List<Long> roleIds);

    /**
     * 根据多个角色ID批量获取按钮ID列表（优化N+1查询）
     * <p>
     * 功能说明：
     * 1. 使用IN语句一次性查询所有角色的按钮关联
     * 2. 返回所有角色关联的按钮ID集合
     * </p>
     *
     * @param roleIds 角色ID列表，不能为空
     * @return 所有角色关联的按钮ID集合
     */
    List<Long> getButtonIdsByRoleIds(List<Long> roleIds);

}
