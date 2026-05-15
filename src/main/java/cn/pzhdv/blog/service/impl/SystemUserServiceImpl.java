package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.SysRole;
import cn.pzhdv.blog.entity.SysUserRole;
import cn.pzhdv.blog.entity.SystemUser;
import cn.pzhdv.blog.exception.BusinessException;
import cn.pzhdv.blog.mapper.SysRoleMapper;
import cn.pzhdv.blog.mapper.SysUserRoleMapper;
import cn.pzhdv.blog.mapper.SystemUserMapper;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.service.SystemUserService;
import cn.pzhdv.blog.utils.QueryWrapperUtil;
import cn.pzhdv.blog.vo.role.SysRoleSimpleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 系统用户表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2025-12-28 16:41:01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemUserServiceImpl extends ServiceImpl<SystemUserMapper, SystemUser> implements SystemUserService {
    private final SystemUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public SystemUser findSystemUserByUserName(String userName) {
        LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SystemUser::getUserName, userName);
        queryWrapper.last("LIMIT 1");
        return userMapper.selectOne(queryWrapper);
    }


    /**
     * 根据用户ID查询用户关联的所有角色（封装到roleList）
     */
    @Override
    public SystemUser getUserWithRoles(Long userId) {
        // 1. 查询用户基础信息
        SystemUser systemUser = userMapper.selectById(userId);
        if (systemUser == null) {
            return null;
        }

        // 2. 查询用户关联的角色ID列表（sys_user_role）
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaEqCondition(userRoleWrapper, SysUserRole::getUserId, userId);
        List<SysUserRole> userRoleList = userRoleMapper.selectList(userRoleWrapper);
        if (CollectionUtils.isEmpty(userRoleList)) {
            systemUser.setRoleList(List.of()); // 无角色时赋值空列表，避免NPE
            systemUser.setRoleIds(List.of()); // 无角色时赋值空列表
            return systemUser;
        }

        // 3. 提取角色ID，查询角色详情（sys_role）
        List<Long> roleIds = userRoleList.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysRole::getRoleId, roleIds);
        List<SysRole> roleList = roleMapper.selectList(roleWrapper);

        // 4. 转换为VO并封装到用户实体
        systemUser.setRoleList(convertToRoleVOList(roleList));
        systemUser.setRoleIds(roleIds); // 同时设置角色ID列表
        return systemUser;
    }


    @Override
    public Page<SystemUser> querySystemUserListByConditionPage(String userName, String userGender, String userNick, String userPhone, String userEmail, Byte status, Integer pageNum, Integer pageSize) {
        // 创建分页对象
        Page<SystemUser> page = new Page<>(pageNum, pageSize);

        // 2. 全Lambda构建查询条件（彻底去除所有硬编码字段）
        LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();

        // 使用 Lambda添加模糊查询条件
        QueryWrapperUtil.addLambdaLikeCondition(queryWrapper, SystemUser::getUserName, userName);
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SystemUser::getUserGender, userGender);
        QueryWrapperUtil.addLambdaLikeCondition(queryWrapper, SystemUser::getUserNick, userNick);
        QueryWrapperUtil.addLambdaLikeCondition(queryWrapper, SystemUser::getUserPhone, userPhone);
        QueryWrapperUtil.addLambdaLikeCondition(queryWrapper, SystemUser::getUserEmail, userEmail);
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SystemUser::getStatus, status);

        // 3. 执行分页查询
        Page<SystemUser> systemUserPage = userMapper.selectPage(page, queryWrapper);

        // 脱敏处理：隐藏密码字段，避免泄露
        systemUserPage.getRecords().forEach(user -> user.setPassword(null));

        return systemUserPage;
    }

    /**
     * 分页查询用户列表时，批量加载每个用户的角色信息
     */
    @Override
    public Page<SystemUser> querySystemUserListWithRolesByConditionPage(String userName, String userGender, String userNick, String userPhone, String userEmail, Byte status, Integer pageNum, Integer pageSize) {

        // 1. 先查询用户分页列表（复用原有逻辑）
        Page<SystemUser> userPage = querySystemUserListByConditionPage(userName, userGender, userNick, userPhone, userEmail, status, pageNum, pageSize);

        List<SystemUser> userList = userPage.getRecords();
        if (CollectionUtils.isEmpty(userList)) {
            return userPage;
        }

        // 2. 批量查询用户-角色关联关系
        List<Long> userIds = userList.stream().map(SystemUser::getUserId).collect(Collectors.toList());

        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.in(SysUserRole::getUserId, userIds);
        List<SysUserRole> userRoleList = userRoleMapper.selectList(userRoleWrapper);
        // 3. 构建「用户ID -> 角色ID列表」的映射
        Map<Long, List<Long>> userId2RoleIdsMap = userRoleList.stream().collect(Collectors.groupingBy(SysUserRole::getUserId, Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));

        // 4. 批量查询所有涉及的角色详情
        List<Long> allRoleIds = userRoleList.stream().map(SysUserRole::getRoleId).distinct().collect(Collectors.toList());

        List<SysRole> roleList = CollectionUtils.isEmpty(allRoleIds) ? List.of() : roleMapper.selectByIds(allRoleIds);

        // 5. 构建「角色ID -> 角色VO」的映射
        Map<Long, SysRoleSimpleVO> roleId2VOMap = convertToRoleVOList(roleList).stream()
                .collect(Collectors.toMap(SysRoleSimpleVO::getRoleId, vo -> vo));

        // 6. 为每个用户封装角色列表和角色ID列表
        for (SystemUser user : userList) {
            Long userId = user.getUserId();
            List<Long> roleIds = userId2RoleIdsMap.getOrDefault(userId, List.of());
            List<SysRoleSimpleVO> userRoleVOList = roleIds.stream().map(roleId2VOMap::get).filter(java.util.Objects::nonNull).collect(Collectors.toList());
            user.setRoleList(userRoleVOList);
            user.setRoleIds(roleIds); // 同时设置角色ID列表
        }

        // 7. 回填封装后的用户列表
        userPage.setRecords(userList);
        return userPage;
    }

    @Override
    public boolean checkUserNameExists(String userName) {
        return checkFieldExists(SystemUser::getUserName, userName);
    }

    @Override
    public boolean checkUserEmailIsExist(String userEmail) {
        return checkFieldExists(SystemUser::getUserEmail, userEmail);
    }

    @Override
    public boolean checkUserPhoneIsExist(String userPhone) {
        return checkFieldExists(SystemUser::getUserPhone, userPhone);
    }

    /**
     * 保存用户并绑定角色（含事务控制）
     *
     * @param systemUser 用户基础信息（含roleIds）
     * @return 保存结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveUser(SystemUser systemUser) {
        // 1. 保存用户基础信息（获取自增的userId）
        boolean userSaveSuccess = userMapper.insert(systemUser) > 0;
        if (!userSaveSuccess) {
            return false;
        }
        Long userId = systemUser.getUserId();
        List<Long> roleIds = systemUser.getRoleIds();

        // 2. 处理角色绑定（roleIds非空时才保存关联关系）
        if (!CollectionUtils.isEmpty(roleIds)) {
            bindRolesToUser(userId, roleIds, "保存用户");
        }
        return true;
    }

    /**
     * 更新用户信息并更新角色绑定（含事务控制）
     *
     * @param systemUser 用户基础信息（含userId和roleIds）
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserById(SystemUser systemUser) {
        Long userId = systemUser.getUserId();
        // 1. 更新用户基础信息
        boolean updateSuccess = userMapper.updateById(systemUser) > 0;
        if (!updateSuccess) {
            log.error("【更新用户】失败 | 数据库更新失败 | 用户ID: {}", userId);
            return false;
        }

        // 2. 处理角色绑定
        List<Long> roleIds = systemUser.getRoleIds();
        
        // 2.1 删除用户原有的角色关联
        deleteUserRoles(userId);
        log.debug("【更新用户】删除原有角色关联 | 用户ID: {}", userId);

        // 2.2 如果传入了新的角色ID列表，则绑定新角色
        if (!CollectionUtils.isEmpty(roleIds)) {
            bindRolesToUser(userId, roleIds, "更新用户");
            log.debug("【更新用户】绑定新角色成功 | 用户ID: {}, 角色数量: {}", userId, roleIds.size());
        } else {
            log.debug("【更新用户】未传入角色ID列表，仅删除原有角色关联 | 用户ID: {}", userId);
        }

        return true;
    }

    /**
     * 通用校验字段是否存在（抽离重复逻辑）
     *
     * @param column 实体字段（如SystemUser::getUserName）
     * @param value  字段值（如用户名/邮箱/手机号）
     * @param <V>    字段值类型
     * @return true=存在，false=不存在
     */
    private <V> boolean checkFieldExists(SFunction<SystemUser, V> column, V value) {
        LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, column, value);
        queryWrapper.select(SystemUser::getUserId); // 固定仅查主键
        Long count = userMapper.selectCount(queryWrapper);
        return count > 0;
    }

    /**
     * 校验角色ID是否存在
     *
     * @param roleIds 角色ID列表
     * @return 存在的角色ID列表
     */
    private List<Long> validateRoleIds(List<Long> roleIds) {
        return roleMapper.selectObjs(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getRoleId, roleIds)
                        .select(SysRole::getRoleId))
                .stream()
                .map(obj -> {
                    // 处理 MyBatis-Plus selectObjs 可能返回 BigInteger 的情况（BIGINT 字段）
                    if (obj instanceof Long) {
                        return (Long) obj;
                    } else if (obj instanceof BigInteger) {
                        return ((BigInteger) obj).longValue();
                    } else if (obj instanceof Number) {
                        return ((Number) obj).longValue();
                    } else {
                        throw new ClassCastException("无法将类型 " + obj.getClass().getName() + " 转换为 Long");
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建用户-角色关联对象列表
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     * @return 用户-角色关联对象列表
     */
    private List<SysUserRole> buildUserRoleList(Long userId, List<Long> roleIds) {
        return roleIds.stream().map(roleId -> {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            return userRole;
        }).collect(Collectors.toList());
    }

    /**
     * 绑定角色到用户（校验并保存角色关联）
     *
     * @param userId     用户ID
     * @param roleIds    角色ID列表
     * @param operation  操作名称（用于日志，如"保存用户"、"更新用户"）
     * @throws BusinessException 角色ID不存在或绑定失败时抛出
     */
    private void bindRolesToUser(Long userId, List<Long> roleIds, String operation) {
        // 1. 校验角色ID是否存在
        List<Long> existRoleIds = validateRoleIds(roleIds);
        if (existRoleIds.size() != roleIds.size()) {
            List<Long> invalidRoleIds = roleIds.stream()
                    .filter(roleId -> !existRoleIds.contains(roleId))
                    .collect(Collectors.toList());
            log.error("【{}】角色ID不存在 | 用户ID: {}, 无效角色ID: {}", operation, userId, invalidRoleIds);
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色ID不存在：" + invalidRoleIds);
        }

        // 2. 批量构建用户-角色关联对象
        List<SysUserRole> userRoleList = buildUserRoleList(userId, roleIds);

        // 3. 批量保存关联关系
        int insertCount = userRoleMapper.batchInsert(userRoleList);
        if (insertCount != userRoleList.size()) {
            String errorMsg = String.format("用户ID[%s]角色绑定异常 | 预期插入[%d]条角色关联数据，实际插入[%d]条",
                    userId, userRoleList.size(), insertCount);
            log.error("【{}】{}", operation, errorMsg);
            ResultCode errorCode = "保存用户".equals(operation) ? ResultCode.ADD_FAIL : ResultCode.UPDATE_FAIL;
            throw new BusinessException(errorCode, errorMsg);
        }
    }

    /**
     * 删除用户的所有角色关联
     *
     * @param userId 用户ID
     * @return 删除的数量
     */
    private int deleteUserRoles(Long userId) {
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId);
        return userRoleMapper.delete(deleteWrapper);
    }

    /**
     * 将角色列表转换为角色VO列表
     *
     * @param roleList 角色列表
     * @return 角色VO列表
     */
    private List<SysRoleSimpleVO> convertToRoleVOList(List<SysRole> roleList) {
        return roleList.stream().map(role -> {
            SysRoleSimpleVO vo = new SysRoleSimpleVO();
            vo.setRoleId(role.getRoleId());
            vo.setRoleName(role.getRoleName());
            vo.setRoleCode(role.getRoleCode());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 根据ID删除用户（含事务控制，同时删除用户关联的角色关系）
     *
     * @param userId 用户ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserById(Long userId) {
        // 1. 先删除用户关联的角色关系
        int deleteRoleCount = deleteUserRoles(userId);
        log.debug("【删除用户】删除用户角色关联 | 用户ID: {}, 删除角色关联数量: {}", userId, deleteRoleCount);

        // 2. 删除用户基础信息
        boolean success = super.removeById(userId);
        if (!success) {
            log.error("【删除用户】失败 | 数据库删除失败 | 用户ID: {}", userId);
        } else {
            log.debug("【删除用户】成功 | 用户ID: {}", userId);
        }
        return success;
    }

    /**
     * 批量删除用户（含事务控制，同时删除用户关联的角色关系）
     *
     * @param userIdList 用户ID列表
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserByIds( List<Long> userIdList) {

        // 1. 批量删除用户关联的角色关系
        int deleteRoleCount = deleteUserRolesBatch(userIdList);
        log.debug("【批量删除用户】删除用户角色关联 | 用户数量: {}, 删除角色关联数量: {}", userIdList.size(), deleteRoleCount);

        // 2. 批量删除用户基础信息
        boolean success = super.removeByIds(userIdList);
        if (!success) {
            log.error("【批量删除用户】失败 | 数据库删除失败 | 用户数量: {}, 用户ID列表: {}", userIdList.size(), userIdList);
        } else {
            log.debug("【批量删除用户】成功 | 用户数量: {}, 用户ID列表: {}", userIdList.size(), userIdList);
        }
        return success;
    }

    /**
     * 批量删除用户的所有角色关联
     *
     * @param userIds 用户ID列表
     * @return 删除的数量
     */
    private int deleteUserRolesBatch(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return 0;
        }
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.in(SysUserRole::getUserId, userIds);
        return userRoleMapper.delete(deleteWrapper);
    }
}
