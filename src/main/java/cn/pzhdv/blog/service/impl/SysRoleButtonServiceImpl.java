package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.SysRoleButton;
import cn.pzhdv.blog.mapper.SysRoleButtonMapper;
import cn.pzhdv.blog.service.SysRoleButtonService;
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
 * 角色-按钮权限关联表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-06 13:39:50
 */
@Service
public class SysRoleButtonServiceImpl extends ServiceImpl<SysRoleButtonMapper, SysRoleButton> implements SysRoleButtonService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> getButtonIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<SysRoleButton> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleButton::getRoleId, roleId);
        List<SysRoleButton> roleButtons = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(roleButtons)) {
            return Collections.emptyList();
        }

        return roleButtons.stream()
                .map(SysRoleButton::getButtonId)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRoleButtons(Long roleId, List<Long> buttonIds) {
        // 1. 删除该角色的所有旧按钮关联
        LambdaQueryWrapper<SysRoleButton> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRoleButton::getRoleId, roleId);
        this.remove(deleteWrapper);

        // 2. 如果按钮ID列表不为空，批量插入新的按钮关联
        if (!CollectionUtils.isEmpty(buttonIds)) {
            List<SysRoleButton> roleButtons = buttonIds.stream()
                    .map(buttonId -> {
                        SysRoleButton roleButton = new SysRoleButton();
                        roleButton.setRoleId(roleId);
                        roleButton.setButtonId(buttonId);
                        return roleButton;
                    })
                    .collect(Collectors.toList());
            return this.saveBatch(roleButtons);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRoleId(Long roleId) {
        LambdaQueryWrapper<SysRoleButton> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRoleButton::getRoleId, roleId);
        return this.remove(deleteWrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return true;
        }
        LambdaQueryWrapper<SysRoleButton> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.in(SysRoleButton::getRoleId, roleIds);
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
     */
    @Override
    public List<Long> getButtonIdsByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysRoleButton> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SysRoleButton::getRoleId, roleIds);
        List<SysRoleButton> roleButtons = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(roleButtons)) {
            return Collections.emptyList();
        }

        return roleButtons.stream()
                .map(SysRoleButton::getButtonId)
                .collect(Collectors.toList());
    }

}
