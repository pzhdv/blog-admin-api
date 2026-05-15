package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.SysRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * <p>
 * 系统角色表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-01 18:53:32
 */
public interface SysRoleService extends IService<SysRole> {

    Page<SysRole> queryRoleListByConditionPage(String roleName, String roleCode, Byte status, Integer current, Integer size);

    boolean checkRoleCodeIsExist(String roleCode);
}
