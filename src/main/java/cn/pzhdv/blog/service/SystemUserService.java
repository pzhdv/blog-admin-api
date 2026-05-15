package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.SystemUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 系统用户业务层接口
 * <p>
 * 核心职责：
 * 1. 封装系统用户的CRUD、分页查询、唯一性校验等核心业务逻辑；
 * 2. 集成用户与角色的关联查询/绑定逻辑，保证数据一致性；
 * 3. 所有修改操作（新增/删除/更新）均包含事务控制，避免数据异常；
 * 4. 提供标准化的参数校验和业务异常抛出，与全局异常处理器联动。
 *
 * @author PanZonghui
 * @since 2025-12-28 16:41:01
 */
public interface SystemUserService extends IService<SystemUser> {

    /**
     * 根据用户名查询系统用户信息
     *
     * @param userName 用户名（不能为空，4-20位字母/数字/下划线）
     * @return SystemUser 匹配的用户基础信息（不含角色），无匹配时返回null
     */
    SystemUser findSystemUserByUserName(String userName);

    /**
     * 分页查询用户列表（包含用户关联的角色信息）
     * <p>优化说明：批量加载角色信息，避免N+1查询，性能优于单用户查询</p>
     * <p>适用场景：用户列表页（需展示角色信息）、权限管理页</p>
     *
     * @param userName   用户名（模糊查询，支持部分匹配，可为null）
     * @param userGender 用户性别（精确匹配，1:男 2:女，可为null）
     * @param userNick   用户昵称（模糊查询，支持部分匹配，可为null）
     * @param userPhone  手机号（模糊查询，支持部分匹配，可为null）
     * @param userEmail  用户邮箱（模糊查询，支持部分匹配，可为null）
     * @param status     用户状态（精确匹配，1:正常 2:禁用，可为null）
     * @param pageNum    页码（不能为空，最小值1，小于1时自动修正为1）
     * @param pageSize   页大小（不能为空，最小值1，最大值建议≤100，超限自动修正为100）
     * @return Page<SystemUser> 分页结果，每条用户记录包含roleList（角色列表）字段
     */
    Page<SystemUser> querySystemUserListWithRolesByConditionPage(
            String userName,
            String userGender,
            String userNick,
            String userPhone,
            String userEmail,
            Byte status,
            Integer pageNum,
            Integer pageSize);

    /**
     * 分页查询用户列表（仅基础信息，不包含角色）
     * <p>适用场景：无需角色信息的轻量查询（如用户下拉选择、简单统计）</p>
     *
     * @param userName   用户名（模糊查询，支持部分匹配，可为null）
     * @param userGender 用户性别（精确匹配，1:男 2:女，可为null）
     * @param userNick   用户昵称（模糊查询，支持部分匹配，可为null）
     * @param userPhone  手机号（模糊查询，支持部分匹配，可为null）
     * @param userEmail  用户邮箱（模糊查询，支持部分匹配，可为null）
     * @param status     用户状态（精确匹配，1:正常 2:禁用，可为null）
     * @param pageNum    页码（不能为空，最小值1，小于1时自动修正为1）
     * @param pageSize   页大小（不能为空，最小值1，最大值建议≤100，超限自动修正为100）
     * @return Page<SystemUser> 分页结果，仅包含用户基础字段（用户名/昵称/手机号等）
     */
    Page<SystemUser> querySystemUserListByConditionPage(
            String userName,
            String userGender,
            String userNick,
            String userPhone,
            String userEmail,
            Byte status,
            Integer pageNum,
            Integer pageSize);

    /**
     * 根据用户ID查询用户信息（包含关联的角色列表）
     * <p>适用场景：用户详情页、用户编辑页、权限分配页</p>
     *
     * @param userId 用户ID（不能为空，正整数，无匹配时返回null）
     * @return SystemUser 包含roleList（角色列表）的完整用户信息，无匹配时返回null
     */
    SystemUser getUserWithRoles(Long userId);

    /**
     * 校验用户名是否已存在
     * <p>业务规则：用户名全局唯一，区分大小写</p>
     *
     * @param userName 用户名（不能为空，4-20位字母/数字/下划线）
     * @return boolean true=已存在，false=不存在
     */
    boolean checkUserNameExists(@NotBlank(message = "用户名不能为空") String userName);

    /**
     * 校验用户邮箱是否已存在
     * <p>业务规则：邮箱全局唯一，不区分大小写</p>
     *
     * @param userEmail 用户邮箱（不能为空，需符合RFC5322邮箱格式）
     * @return boolean true=已存在，false=不存在
     */
    boolean checkUserEmailIsExist(@NotBlank(message = "用户邮箱不能为空") String userEmail);

    /**
     * 校验用户手机号是否已存在
     * <p>业务规则：手机号全局唯一，仅支持11位国内手机号</p>
     *
     * @param userPhone 用户手机号（不能为空，11位纯数字，格式为1[3-9]\\d{9}）
     * @return boolean true=已存在，false=不存在
     */
    boolean checkUserPhoneIsExist(@NotBlank(message = "用户手机号不能为空") String userPhone);

    /**
     * 新增用户（含角色绑定）
     * <p>事务控制：用户新增+角色绑定为原子操作，要么全成功，要么全回滚</p>
     * <p>业务规则：
     * 1. 密码需提前加密（建议BCrypt算法）；
     * 2. 角色ID需存在，否则抛出BusinessException；
     * 3. 用户名/手机号/邮箱需提前校验唯一性</p>
     *
     * @param systemUser 用户信息（包含roleIds角色ID列表，userId无需传值，自动生成）
     * @return boolean true=新增成功，false=新增失败（如数据库操作异常）
     * @throws cn.pzhdv.blog.exception.BusinessException 角色ID不存在/绑定失败时抛出
     */
    boolean saveUser(SystemUser systemUser);

    /**
     * 批量删除用户
     * <p>事务控制：批量删除为原子操作，要么全成功，要么全回滚</p>
     * <p>业务规则：
     * 1. 逻辑删除/物理删除由实体类@TableLogic注解控制；
     * 2. 若用户关联有业务数据（如订单、日志），禁止删除（需提前校验）；
     * 3. 删除用户同时删除用户-角色关联关系</p>
     *
     * @param userIdList 用户ID列表（不能为空，且需为有效正整数，空列表返回false）
     * @return boolean true=删除成功，false=删除失败（如无有效用户ID/数据库异常）
     * @throws cn.pzhdv.blog.exception.BusinessException 用户关联业务数据无法删除时抛出
     */
    boolean deleteUserByIds(List<Long> userIdList);

    /**
     * 单个删除用户
     * <p>底层复用批量删除逻辑，仅封装单ID场景</p>
     *
     * @param userId 用户ID（不能为空，正整数，无匹配时返回false）
     * @return boolean true=删除成功，false=删除失败（如用户不存在/数据库异常）
     * @throws cn.pzhdv.blog.exception.BusinessException 用户关联业务数据无法删除时抛出
     */
    boolean deleteUserById(Long userId);

    /**
     * 修改用户信息（支持角色重新绑定）
     * <p>事务控制：用户信息更新+角色重新绑定为原子操作</p>
     * <p>业务规则：
     * 1. 用户名不允许修改（若需修改需重新校验唯一性）；
     * 2. 密码若不为空则视为更新，需重新加密；
     * 3. 角色ID列表为空则清空该用户所有角色</p>
     *
     * @param systemUser 用户信息（userId不能为空，为修改主键；roleIds为新的角色列表）
     * @return boolean true=更新成功，false=更新失败（如用户不存在/数据库异常）
     * @throws cn.pzhdv.blog.exception.BusinessException 角色ID不存在/用户名重复时抛出
     */
    boolean updateUserById(SystemUser systemUser);
}