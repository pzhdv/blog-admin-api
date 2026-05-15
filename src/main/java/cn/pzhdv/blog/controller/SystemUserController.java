package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.constant.RedisKey;
import cn.pzhdv.blog.dto.common.BatchDeleteReq;
import cn.pzhdv.blog.dto.system.user.UserRegisterDTO;
import cn.pzhdv.blog.dto.system.user.UserUpdateDTO;
import cn.pzhdv.blog.entity.SystemUser;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.SysUserRoleService;
import cn.pzhdv.blog.service.SystemUserService;
import cn.pzhdv.blog.utils.CacheExpireUtil;
import cn.pzhdv.blog.utils.Md5Util;
import cn.pzhdv.blog.utils.PasswordUtil;
import cn.pzhdv.blog.utils.RedisUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Objects;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 系统用户表 前端控制器 管理系统用户的登录、注册、信息维护等功能
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Slf4j
@Validated
@Api(tags = "系统用户管理")
@RestController
@RequestMapping("/systemManage/systemUser")
@RequiredArgsConstructor
public class SystemUserController {

    private final SystemUserService baseService;

    private final SysUserRoleService sysUserRoleService;

    private final RedisUtils redisUtils;


    @ApiLog("分页查询用户列表")
    @ApiOperation(
            value = "用户列表条件分页查询",
            notes = "支持用户名、性别、昵称、手机号、邮箱、用户状态等条件，默认分页10条,页页码≥1，每页条数≥1（无上限）",
            produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "userName",
                    value = "用户名（模糊匹配）",
                    paramType = "query",
                    dataType = "String",
                    dataTypeClass = String.class),
            @ApiImplicitParam(
                    name = "userGender",
                    value = "用户性别（1:男 2:女，精准匹配）",
                    paramType = "query",
                    dataType = "String",
                    dataTypeClass = String.class),
            @ApiImplicitParam(
                    name = "userNick",
                    value = "用户昵称（模糊匹配）",
                    paramType = "query",
                    dataType = "String",
                    dataTypeClass = String.class),
            @ApiImplicitParam(
                    name = "userPhone",
                    value = "手机号（模糊匹配）",
                    paramType = "query",
                    dataType = "String",
                    dataTypeClass = String.class),
            @ApiImplicitParam(
                    name = "userEmail",
                    value = "用户邮箱（模糊匹配）",
                    paramType = "query",
                    dataType = "String",
                    dataTypeClass = String.class),
            @ApiImplicitParam(
                    name = "status",
                    value = "用户状态（1:正常 2:禁止，精准匹配）",
                    paramType = "query",
                    dataType = "Byte",
                    dataTypeClass = Byte.class),
            @ApiImplicitParam(
                    name = "current",
                    value = "当前页码（≥1）",
                    paramType = "query",
                    required = true,
                    dataType = "Integer",
                    dataTypeClass = Integer.class,
                    defaultValue = "1"),
            @ApiImplicitParam(
                    name = "size",
                    value = "每页条数（≥1，无上限）",
                    paramType = "query",
                    required = true,
                    dataType = "Integer",
                    dataTypeClass = Integer.class,
                    defaultValue = "10")
    })
    @GetMapping("getUserList")
    public Result<Page<SystemUser>> getUserList(
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "userGender", required = false) String userGender,
            @RequestParam(value = "userNick", required = false) String userNick,
            @RequestParam(value = "userPhone", required = false) String userPhone,
            @RequestParam(value = "userEmail", required = false) String userEmail,
            @RequestParam(value = "status", required = false) Byte status,
            @RequestParam(value = "current", defaultValue = "1") @Min(value = 1, message = "页码必须≥1")
            Integer current,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数必须≥1")
            Integer size) {
        // 生成缓存 key
        String cacheKey = RedisKey.SYSTEM_USER_PAGE_KEY + buildUserListCacheKey(userName, userGender, userNick, userPhone, userEmail, status, current, size);

        // 尝试从缓存获取
        Page<SystemUser> cachedPage = redisUtils.get(cacheKey, new TypeReference<>() {
        });
        if (cachedPage != null) {
            log.debug("【用户列表】命中缓存 | key: {}", cacheKey);
            return ResultUtil.ok(cachedPage);
        }

        Page<SystemUser> systemUserPage =
                baseService.querySystemUserListWithRolesByConditionPage(
                        userName, userGender, userNick, userPhone, userEmail, status, current, size);
        log.info(
                "分页查询系统用户列表成功，条件：userName={}, userGender={}, userNick={}, userPhone={}, userEmail={}, status={}, 页码={}, 每页条数={}",
                userName, userGender, userNick, userPhone, userEmail, status, current, size);

        // 写入缓存
        redisUtils.set(cacheKey, systemUserPage, CacheExpireUtil.getDefaultExpireSeconds());

        return ResultUtil.ok(systemUserPage);
    }

    /**
     * 构建用户列表缓存 key（对查询参数进行 MD5 哈希压缩）
     */
    private String buildUserListCacheKey(String userName, String userGender, String userNick,
                                         String userPhone, String userEmail, Byte status,
                                         Integer current, Integer size) {
        return Md5Util.md5Of(userName, userGender, userNick, userPhone, userEmail, status, current, size);
    }

    @ApiLog("根据ID获取用户详情")
    @ApiOperation(
            value = "根据 ID获取用户详细信息",
            notes = "根据用户 ID查询用户详细信息",
            produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "userId",
                    value = "系统用户ID（必须为正整数）",
                    paramType = "query",
                    dataType = "Long",
                    required = true,
                    example = "1001",
                    dataTypeClass = Long.class)
    })
    @GetMapping("getUserInfoById")
    public Result<SystemUser> getUserInfoById(
            @RequestParam(value = "userId")
            @Min(value = 1, message = "用户 ID必须为正整数")
            Long userId) {

        // 尝试从缓存获取
        String cacheKey = RedisKey.SYSTEM_USER_DETAIL_KEY + userId;
        SystemUser cachedUser = redisUtils.get(cacheKey, SystemUser.class);
        if (cachedUser != null) {
            cachedUser.setPassword(null);
            log.debug("【获取用户信息】命中缓存 | 用户ID: {}", userId);
            return ResultUtil.ok(cachedUser);
        }

        // 查询用户信息
        SystemUser user = baseService.getUserWithRoles(userId);
        if (user == null) {
            log.warn("【获取用户信息】失败 | 用户不存在 | 用户ID: {}", userId);
            return ResultUtil.error(ResultCode.USER_NOT_EXIST, "用户不存在");
        }

        // 去除密码敏感信息
        user.setPassword(null);

        // 写入缓存（不含密码）
        redisUtils.set(cacheKey, user, CacheExpireUtil.getDefaultExpireSeconds());

        log.debug("【获取用户信息】成功 | 用户ID: {}, 用户名: {}, 昵称: {}", userId, user.getUserName(), user.getUserNick());
        return ResultUtil.ok(user);
    }

    @ApiLog("校验用户名是否存在")
    @ApiOperation(
            value = "检查用户名是否存在",
            notes = "检查用户用户名是否存在",
            produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "userName",
                    value = "用户名",
                    paramType = "query",
                    dataType = "String",
                    required = true,
                    dataTypeClass = String.class)
    })
    @GetMapping("checkUserNameExists")
    public Result<Boolean> checkUserNameExist(
            @RequestParam(value = "userName") @NotBlank(message = "用户名不能为空")
            String userName) {
        // 尝试从缓存获取
        String cacheKey = RedisKey.SYSTEM_USER_USERNAME_EXISTS_KEY + userName;
        Boolean cached = redisUtils.get(cacheKey, Boolean.class);
        if (cached != null) {
            log.debug("【检查用户名】命中缓存 | 用户名: {}, 存在: {}", userName, cached);
            return ResultUtil.ok(cached);
        }

        // 检查用户名是否存在
        boolean exists = baseService.checkUserNameExists(userName);
        log.info("用户名{} | 用户名: {}", exists ? "已存在" : "不存在", userName);

        // 写入缓存（不存在时缓存 false，避免缓存穿透）
        redisUtils.set(cacheKey, exists, CacheExpireUtil.getDefaultExpireSeconds());

        return ResultUtil.ok(exists);
    }

    @ApiLog("校验用户邮箱是否存在")
    @ApiOperation(
            value = "检查用户邮箱是否存在",
            notes = "检查用户邮箱是否存在",
            produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "userEmail",
                    value = "用户邮箱",
                    paramType = "query",
                    dataType = "String",
                    required = true,
                    dataTypeClass = String.class)
    })
    @GetMapping("checkUserEmailExists")
    public Result<Boolean> checkUserEmailIsExist(
            @RequestParam(value = "userEmail") @NotBlank(message = "用户邮箱不能为空")
            String userEmail) {
        // 尝试从缓存获取
        String cacheKey = RedisKey.SYSTEM_USER_EMAIL_EXISTS_KEY + userEmail;
        Boolean cached = redisUtils.get(cacheKey, Boolean.class);
        if (cached != null) {
            log.debug("【检查用户邮箱】命中缓存 | 用户邮箱: {}, 存在: {}", userEmail, cached);
            return ResultUtil.ok(cached);
        }

        // 检查用户邮箱是否存在
        boolean exists = baseService.checkUserEmailIsExist(userEmail);
        log.info("用户邮箱{} | 用户邮箱: {}", exists ? "已存在" : "不存在", userEmail);

        // 写入缓存
        redisUtils.set(cacheKey, exists, CacheExpireUtil.getDefaultExpireSeconds());

        return ResultUtil.ok(exists);
    }

    @ApiLog("校验用户手机号是否存在")
    @ApiOperation(
            value = "检查用户手机号是否存在",
            notes = "检查用户手机号是否存在",
            produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "userPhone",
                    value = "用户手机号",
                    paramType = "query",
                    dataType = "String",
                    required = true,
                    dataTypeClass = String.class)
    })
    @GetMapping("checkUserPhoneIsExist")
    public Result<Boolean> checkUserPhoneIsExist(
            @RequestParam(value = "userPhone") @NotBlank(message = "用户手机号不能为空")
            String userPhone) {
        // 尝试从缓存获取
        String cacheKey = RedisKey.SYSTEM_USER_PHONE_EXISTS_KEY + userPhone;
        Boolean cached = redisUtils.get(cacheKey, Boolean.class);
        if (cached != null) {
            log.debug("【检查用户手机号】命中缓存 | 用户手机号: {}, 存在: {}", userPhone, cached);
            return ResultUtil.ok(cached);
        }

        // 检查用户手机号是否存在
        boolean exists = baseService.checkUserPhoneIsExist(userPhone);
        log.info("用户手机号{} | 用户手机号: {}", exists ? "已存在" : "不存在", userPhone);

        // 写入缓存
        redisUtils.set(cacheKey, exists, CacheExpireUtil.getDefaultExpireSeconds());

        return ResultUtil.ok(exists);
    }


    @ApiLog("用户注册")
    @ApiOperation(
            value = "用户注册",
            notes = "系统用户注册（用户名唯一，密码自动加密存储）",
            produces = "application/json")
    @ApiOperationSupport(ignoreParameters = {"userId"})
    @PostMapping("register")
    public Result<Boolean> register(@RequestBody @Validated UserRegisterDTO registerDTO) {
        String userName = registerDTO.getUserName();
        String userPhone = registerDTO.getUserPhone();
        String userEmail = registerDTO.getUserEmail();
        String rawPassword = registerDTO.getPassword();

        SystemUser systemUser = new SystemUser();
        BeanUtils.copyProperties(registerDTO, systemUser);

        // 1. 参数唯一性校验（已通过格式校验，此处检查是否重复）
        if (baseService.checkUserNameExists(userName)) {
            log.warn("【用户注册】失败 | 用户名已被占用 | 用户名: {}", userName);
            return ResultUtil.error(ResultCode.DUPLICATE_DATA.getCode(), "用户名[" + userName + "]已被占用");
        }
        // 手机号唯一性校验（DTO中已通过@NotBlank保证非空）
        if (baseService.checkUserPhoneIsExist(userPhone)) {
            log.warn("【用户注册】失败 | 手机号已被占用 | 手机号: {}", userPhone);
            return ResultUtil.error(ResultCode.DUPLICATE_DATA.getCode(), "手机号[" + userPhone + "]已被占用");
        }
        // 邮箱唯一性校验（DTO中已通过@NotBlank保证非空）
        if (baseService.checkUserEmailIsExist(userEmail)) {
            log.warn("【用户注册】失败 | 邮箱已被占用 | 邮箱: {}", userEmail);
            return ResultUtil.error(ResultCode.DUPLICATE_DATA.getCode(), "邮箱[" + userEmail + "]已被占用");
        }


        // 2. 密码加密（异常返回服务器错误）
        try {
            systemUser.setPassword(PasswordUtil.hashPassword(rawPassword));
        } catch (Exception e) {
            log.error("【用户注册】失败 | 密码加密异常 | 用户名: {}", userName, e);
            return ResultUtil.error(ResultCode.SERVER_ERROR, "密码加密异常，请重试");
        }

        // 3. 保存用户（失败返回注册失败）
        boolean success = baseService.saveUser(systemUser);
        if (!success) {
            log.error("【用户注册】失败 | 数据库保存失败 | 用户名: {}", userName);
            return ResultUtil.error(ResultCode.REGISTER_FAIL, "注册失败，请稍后重试");
        }

        // 注册成功
        log.info("【用户注册】成功 | 用户名: {}, 用户ID: {}, 手机号: {}, 邮箱: {}", userName, systemUser.getUserId(), userPhone, userEmail);

        // 清除用户列表缓存
        redisUtils.deleteKeysByPattern(RedisKey.SYSTEM_USER_PAGE_KEY + "*");
        // 清除用户名/邮箱/手机号存在性缓存
        redisUtils.del(RedisKey.SYSTEM_USER_USERNAME_EXISTS_KEY + userName);
        redisUtils.del(RedisKey.SYSTEM_USER_EMAIL_EXISTS_KEY + userEmail);
        redisUtils.del(RedisKey.SYSTEM_USER_PHONE_EXISTS_KEY + userPhone);

        return ResultUtil.ok(true);
    }

    @ApiLog("修改用户信息")
    @ApiOperation(
            value = "修改用户信息",
            notes = "更新系统用户信息（用户名/手机号/邮箱需全局唯一，支持修改用户名）",
            produces = "application/json")
    @PutMapping("update")
    public Result<Boolean> update(
            @RequestBody
            @Validated
            @ApiParam(value = "用户信息（必须包含userId）", required = true)
            UserUpdateDTO userUpdateDTO) {

        // 1. 校验用户是否存在（userId非空由DTO的@NotNull保证）
        Long userId = userUpdateDTO.getUserId();
        SystemUser existingUser = baseService.getById(userId);
        if (existingUser == null) {
            log.warn("【修改用户信息】失败 | 用户不存在 | 用户ID: {}", userId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "用户不存在，无法修改");
        }

        // 2. 构建更新实体
        SystemUser systemUser = new SystemUser();
        BeanUtils.copyProperties(userUpdateDTO, systemUser);

        // 3. 唯一性校验（排除当前用户自身）
        // 3.1 用户名唯一性校验（若修改了用户名）
        if (!Objects.equals(userUpdateDTO.getUserName(), existingUser.getUserName())) {
            // 用户名格式由DTO的@Pattern保证，此处仅校验唯一性
            if (baseService.checkUserNameExists(userUpdateDTO.getUserName())) {
                log.warn("【修改用户信息】失败 | 用户名已被占用 | 用户ID: {}, 新用户名: {}",
                        userId, userUpdateDTO.getUserName());
                return ResultUtil.error(ResultCode.DUPLICATE_DATA.getCode(), "用户名[" + userUpdateDTO.getUserName() + "]已被占用");
            }
        }

        // 3.2 手机号唯一性校验（若修改了手机号）
        if (!Objects.equals(userUpdateDTO.getUserPhone(), existingUser.getUserPhone())) {
            if (baseService.checkUserPhoneIsExist(userUpdateDTO.getUserPhone())) {
                log.warn("【修改用户信息】失败 | 手机号已被占用 | 用户ID: {}, 新手机号: {}",
                        userId, userUpdateDTO.getUserPhone());
                return ResultUtil.error(ResultCode.DUPLICATE_DATA.getCode(), "手机号[" + userUpdateDTO.getUserPhone() + "]已被占用");
            }
        }

        // 3.3 邮箱唯一性校验（若修改了邮箱）
        if (!Objects.equals(userUpdateDTO.getUserEmail(), existingUser.getUserEmail())) {
            if (baseService.checkUserEmailIsExist(userUpdateDTO.getUserEmail())) {
                log.warn("【修改用户信息】失败 | 邮箱已被占用 | 用户ID: {}, 新邮箱: {}",
                        userId, userUpdateDTO.getUserEmail());
                return ResultUtil.error(ResultCode.DUPLICATE_DATA.getCode(), "邮箱[" + userUpdateDTO.getUserEmail() + "]已被占用");
            }
        }

        //  处理密码（仅保留密码字段的空值逻辑）
        String newPassword = userUpdateDTO.getPassword();
        if (StringUtils.hasText(newPassword)) {
            try {
                systemUser.setPassword(PasswordUtil.hashPassword(newPassword));
                log.info("【修改用户信息】密码将更新 | 用户ID: {}", userId);
            } catch (Exception e) {
                log.error("【修改用户信息】失败 | 密码加密异常 | 用户ID: {}", userId, e);
                return ResultUtil.error(ResultCode.SERVER_ERROR, "密码加密异常，请重试");
            }
        } else {
            // 密码为空时复用原密码（避免清空密码）
            systemUser.setPassword(existingUser.getPassword());
        }

        // 执行修改操作
        boolean success = baseService.updateUserById(systemUser);
        if (!success) {
            log.error("【修改用户信息】失败 | 数据库更新失败 | 用户ID: {}, 新用户名: {}",
                    userId, userUpdateDTO.getUserName());
            return ResultUtil.error(ResultCode.UPDATE_FAIL, "修改失败，请稍后重试");
        }

        //  日志记录
        log.info(
                "【修改用户信息】成功 | 用户ID: {}, 原用户名: {}, 新用户名: {}, 手机号: {}, 邮箱: {}",
                userId, existingUser.getUserName(), userUpdateDTO.getUserName(),
                userUpdateDTO.getUserPhone(), userUpdateDTO.getUserEmail()
        );

        // 清除缓存
        redisUtils.del(RedisKey.SYSTEM_USER_DETAIL_KEY + userId);
        redisUtils.deleteKeysByPattern(RedisKey.SYSTEM_USER_PAGE_KEY + "*");
        redisUtils.del(RedisKey.SYSTEM_USER_USERNAME_EXISTS_KEY + existingUser.getUserName());
        redisUtils.del(RedisKey.SYSTEM_USER_USERNAME_EXISTS_KEY + userUpdateDTO.getUserName());
        redisUtils.del(RedisKey.SYSTEM_USER_EMAIL_EXISTS_KEY + existingUser.getUserEmail());
        redisUtils.del(RedisKey.SYSTEM_USER_EMAIL_EXISTS_KEY + userUpdateDTO.getUserEmail());
        redisUtils.del(RedisKey.SYSTEM_USER_PHONE_EXISTS_KEY + existingUser.getUserPhone());
        redisUtils.del(RedisKey.SYSTEM_USER_PHONE_EXISTS_KEY + userUpdateDTO.getUserPhone());

        return ResultUtil.ok(true);
    }

    @ApiLog("删除系统用户")
    @ApiOperation(
            value = "删除系统用户",
            notes = "根据用户ID删除系统用户（谨慎操作，会影响登录权限）",
            produces = "application/json")
    @DeleteMapping("delete/{userId}")
    public Result<Boolean> deleteById(
            @PathVariable @ApiParam(name = "userId", value = "系统用户ID（≥1）", required = true)
            @Min(value = 1, message = "用户ID必须为正整数")
            Long userId) {

        // 1. 校验用户是否存在
        SystemUser user = baseService.getById(userId);
        if (user == null) {
            log.warn("删除失败 | 用户不存在 | 用户ID: {}", userId);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "用户不存在，无法删除");
        }

        // 2. 删除用户角色中间表中的关联数据（级联删除）
        boolean userRoleDeleteSuccess = sysUserRoleService.deleteByUserId(userId);
        if (!userRoleDeleteSuccess) {
            log.error("【删除用户】操作失败 | 原因：删除用户角色关联失败 | 用户ID：{} | 用户名：{} ", userId, user.getUserName());
            return ResultUtil.error(ResultCode.DELETE_FAIL, "删除用户角色关联失败，请稍后重试");
        }

        // 3. 执行用户物理删除
        boolean success = baseService.deleteUserById(userId);
        if (!success) {
            log.error("【删除系统用户】操作失败 | 原因：数据库删除失败 | 用户ID：{} | 用户名：{}",
                    userId, user.getUserName());
            return ResultUtil.error(ResultCode.DELETE_FAIL, "数据库操作异常，删除失败，请稍后重试");
        }

        log.info("【删除系统用户】操作成功 | 用户ID：{} | 用户名：{} | 账号：{}",
                userId, user.getUserName(), user.getUserName());

        // 清除缓存
        redisUtils.del(RedisKey.SYSTEM_USER_DETAIL_KEY + userId);
        redisUtils.deleteKeysByPattern(RedisKey.SYSTEM_USER_PAGE_KEY + "*");
        redisUtils.del(RedisKey.SYSTEM_USER_USERNAME_EXISTS_KEY + user.getUserName());
        redisUtils.del(RedisKey.SYSTEM_USER_EMAIL_EXISTS_KEY + user.getUserEmail());
        redisUtils.del(RedisKey.SYSTEM_USER_PHONE_EXISTS_KEY + user.getUserPhone());

        return ResultUtil.ok(true);
    }

    @ApiLog("批量删除系统用户")
    @ApiOperation(
            value = "批量删除系统用户",
            notes = "批量删除系统用户（谨慎操作，ID≥1）",
            produces = "application/json")
    @DeleteMapping("delete/batch")
    public Result<Boolean> deleteBatch(
            @RequestBody @Valid
            BatchDeleteReq deleteReq) {

        // 1. 获取用户ID列表
        List<Long> userIdList = deleteReq.getIds();

        // 2. 空列表校验
        if (userIdList.isEmpty()) {
            log.warn("批量删除失败 | 待删除用户ID列表为空");
            return ResultUtil.error(ResultCode.DELETE_FAIL, "待删除用户ID列表不能为空");
        }

        // 3. 批量校验用户是否存在
        List<SystemUser> existUsers = baseService.listByIds(userIdList);
        List<Long> existUserIdList = existUsers.stream()
                .map(SystemUser::getUserId)
                .toList();

        // 4. 筛选不存在的用户ID
        List<Long> notExistIds = userIdList.stream()
                .filter(id -> !existUserIdList.contains(id))
                .toList();

        // 5. 存在不存在的ID则返回提示（原子性：有一个不存在就不执行删除）
        if (!notExistIds.isEmpty()) {
            log.warn("批量删除失败 | 部分用户ID不存在 | 不存在的ID列表：{}", notExistIds);
            return ResultUtil.error(
                    ResultCode.DATA_NOT_FOUND,
                    String.format("以下ID对应的用户不存在：%s", notExistIds)
            );
        }

        // 6. 批量删除用户角色中间表关联数据
        boolean userRoleDeleteSuccess = sysUserRoleService.deleteByUserIds(userIdList);
        if (!userRoleDeleteSuccess) {
            log.error("【批量删除用户】删除失败 | 删除用户角色关联失败 | 待删除用户数量：{} | 用户ID列表：{}",
                    userIdList.size(), userIdList);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "删除用户角色关联失败，请稍后重试");
        }

        // 7. 执行批量删除用户
        boolean success = baseService.deleteUserByIds(userIdList);
        if (!success) {
            log.error("【批量删除用户】删除失败 | 数据库操作失败 | 待删除用户数量：{} | 用户ID列表：{}",
                    userIdList.size(), userIdList);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "数据库操作异常，请稍后重试");
        }

        // 8. 拼接成功日志
        List<String> deletedUserNames = existUsers.stream()
                .map(SystemUser::getUserName)
                .toList();
        log.info("【批量删除用户】删除成功 | 已删除用户数量：{} | 用户ID列表：{} | 用户名列表：{}",
                userIdList.size(), userIdList, deletedUserNames);

        // 清除缓存
        userIdList.forEach(id -> redisUtils.del(RedisKey.SYSTEM_USER_DETAIL_KEY + id));
        redisUtils.deleteKeysByPattern(RedisKey.SYSTEM_USER_PAGE_KEY + "*");
        existUsers.forEach(user -> {
            redisUtils.del(RedisKey.SYSTEM_USER_USERNAME_EXISTS_KEY + user.getUserName());
            redisUtils.del(RedisKey.SYSTEM_USER_EMAIL_EXISTS_KEY + user.getUserEmail());
            redisUtils.del(RedisKey.SYSTEM_USER_PHONE_EXISTS_KEY + user.getUserPhone());
        });

        return ResultUtil.ok(true);
    }
}