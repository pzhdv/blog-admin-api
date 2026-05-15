package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.SysButton;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 系统菜单按钮权限表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-05 19:36:10
 */
public interface SysButtonService extends IService<SysButton> {

    /**
     * 条件分页查询菜单按钮权限列表
     *
     * @param menuId  所属菜单ID（精准匹配，可为空）
     * @param status  按钮状态（1:正常 2:禁用，精准匹配，可为空）
     * @param current 当前页码（≥1）
     * @param size    每页条数（≥1）
     * @return 分页结果
     */
    Page<SysButton> querySysButtonListByConditionPage(Long menuId, Byte status, Integer current, Integer size);

    /**
     * 校验按钮编码是否存在
     *
     * @param buttonCode 按钮编码
     * @return true表示已存在，false表示不存在
     */
    boolean checkButtonCodeExists(String buttonCode);


    /**
     * 查询菜单按钮权限列表
     *
     * @param menuId  所属菜单ID（精准匹配，可为空）
     * @param status  按钮状态（1:正常 2:禁用，精准匹配，可为空）
     * @return 列表
     */
    List<SysButton> querySysButtonListByCondition(Long menuId, Byte status);
}
