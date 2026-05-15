package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.SysRoleMenu;
import cn.pzhdv.blog.mapper.SysRoleMenuMapper;
import cn.pzhdv.blog.service.SysRoleMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色菜单关联中间表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-04 23:44:17
 */
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {

    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：
     * 1. 根据角色ID查询角色菜单关联表
     * 2. 提取所有菜单ID
     * 3. 返回菜单ID列表
     * </p>
     */
    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<SysRoleMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> roleMenus = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(roleMenus)) {
            return Collections.emptyList();
        }

        return roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：
     * 1. 先删除该角色的所有旧菜单关联
     * 2. 如果菜单ID列表不为空，批量插入新的菜单关联
     * 3. 使用事务保证数据一致性
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRoleMenus(Long roleId, List<Long> menuIds) {
        // 1. 删除该角色的所有旧菜单关联
        LambdaQueryWrapper<SysRoleMenu> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRoleMenu::getRoleId, roleId);
        this.remove(deleteWrapper);

        // 2. 如果菜单ID列表不为空，批量插入新的菜单关联
        if (!CollectionUtils.isEmpty(menuIds)) {
            List<SysRoleMenu> roleMenus = menuIds.stream()
                    .map(menuId -> {
                        SysRoleMenu roleMenu = new SysRoleMenu();
                        roleMenu.setRoleId(roleId);
                        roleMenu.setMenuId(menuId);
                        return roleMenu;
                    })
                    .collect(Collectors.toList());
            return this.saveBatch(roleMenus);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：
     * 1. 根据角色ID删除该角色的所有菜单关联记录
     * 2. 用于角色删除时清理关联数据
     * 3. 如果角色本来就没有关联菜单（删除0条记录），也视为成功，因为目标状态已达成
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRoleId(Long roleId) {
        LambdaQueryWrapper<SysRoleMenu> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRoleMenu::getRoleId, roleId);
        // 先查询是否存在关联记录
        long count = this.count(deleteWrapper);
        if (count == 0) {
            // 如果没有关联记录，直接返回true（目标状态已达成）
            return true;
        }
        // 有记录则执行删除
        return this.remove(deleteWrapper);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：
     * 1. 根据角色ID列表批量删除这些角色的所有菜单关联记录
     * 2. 用于批量删除角色时清理关联数据
     * 3. 如果角色本来就没有关联菜单（删除0条记录），也视为成功，因为目标状态已达成
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return true;
        }
        LambdaQueryWrapper<SysRoleMenu> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.in(SysRoleMenu::getRoleId, roleIds);
        // 先查询是否存在关联记录
        long count = this.count(deleteWrapper);
        if (count == 0) {
            // 如果没有关联记录，直接返回true（目标状态已达成）
            return true;
        }
        // 有记录则执行删除
        return this.remove(deleteWrapper);
    }

    /**
     * 根据多个角色ID批量获取菜单ID列表（优化N+1查询）
     */
    @Override
    public List<Long> getMenuIdsByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysRoleMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SysRoleMenu::getRoleId, roleIds);
        List<SysRoleMenu> roleMenus = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(roleMenus)) {
            return Collections.emptyList();
        }

        return roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }
}
