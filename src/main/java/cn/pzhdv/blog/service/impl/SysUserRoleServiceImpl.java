package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.SysUserRole;
import cn.pzhdv.blog.mapper.SysUserRoleMapper;
import cn.pzhdv.blog.service.SysUserRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 用户角色关联表（多对多） 服务实现类
 * </p>
 * <p>
 * 实现用户与角色关联关系的删除操作，核心特性：
 * 1. 单用户/批量用户的角色关联删除
 * 2. 无关联记录时仍返回成功（目标状态已达成）
 * 3. 空值校验，避免无效查询
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02 00:12:29
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：
     * 1. 根据用户ID删除该用户的所有角色关联记录
     * 2. 用于用户删除时清理关联数据
     * 3. 如果用户本来就没有关联角色（删除0条记录），也视为成功，因为目标状态已达成
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByUserId(Long userId) {
        // 空值校验：用户ID为空时直接返回成功（无数据可删）
        if (userId == null) {
            return true;
        }

        // 构建删除条件：根据用户ID精准匹配
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId);

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
     * 1. 根据用户ID列表批量删除这些用户的所有角色关联记录
     * 2. 用于批量删除用户时清理关联数据
     * 3. 如果用户本来就没有关联角色（删除0条记录），也视为成功，因为目标状态已达成
     * 4. 空列表直接返回成功，避免无效查询
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByUserIds(List<Long> userIdList) {
        // 空列表校验：用户ID列表为空时直接返回成功（无数据可删）
        if (CollectionUtils.isEmpty(userIdList)) {
            return true;
        }

        // 构建删除条件：根据用户ID列表批量匹配
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.in(SysUserRole::getUserId, userIdList);

        // 先查询是否存在关联记录
        long count = this.count(deleteWrapper);
        if (count == 0) {
            // 如果没有关联记录，直接返回true（目标状态已达成）
            return true;
        }

        // 有记录则执行批量删除
        return this.remove(deleteWrapper);
    }
}