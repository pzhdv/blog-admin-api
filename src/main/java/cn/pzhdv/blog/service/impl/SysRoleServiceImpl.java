package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.SysRole;
import cn.pzhdv.blog.mapper.SysRoleMapper;
import cn.pzhdv.blog.service.SysRoleService;
import cn.pzhdv.blog.utils.QueryWrapperUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统角色表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-01 18:53:32
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    @Override
    public Page<SysRole> queryRoleListByConditionPage(String roleName, String roleCode, Byte status, Integer current, Integer size) {
        // 创建分页对象
        Page<SysRole> page = new Page<>(current, size);

        // 2. 全Lambda构建查询条件（彻底去除所有硬编码字段）
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();

        // 使用 Lambda添加模糊查询条件
        QueryWrapperUtil.addLambdaLikeCondition(queryWrapper, SysRole::getRoleName, roleName);
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SysRole::getRoleCode, roleCode);
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SysRole::getStatus, status);

        // 3. 执行分页查询
        return sysRoleMapper.selectPage(page, queryWrapper);
    }

    @Override
    public boolean checkRoleCodeIsExist(String roleCode) {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SysRole::getRoleCode, roleCode);
        queryWrapper.select(SysRole::getRoleId);
        Long count = sysRoleMapper.selectCount(queryWrapper);
        return count > 0;
    }
}
