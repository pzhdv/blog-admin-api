package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.SysUserRole;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.constraints.Min;
import java.util.List;

/**
 * <p>
 * 用户角色关联表（多对多） 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02 00:12:29
 */
public interface SysUserRoleService extends IService<SysUserRole> {

    boolean deleteByUserId(@Min(value = 1, message = "用户ID必须为正整数") Long userId);

    boolean deleteByUserIds(List<Long> userIdList);
}
