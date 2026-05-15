package cn.pzhdv.blog.mapper;

import cn.pzhdv.blog.entity.SysUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户角色关联表（多对多） Mapper 接口
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02 00:12:29
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 批量插入用户-角色关联数据（XML版）
     * @param list 用户-角色关联列表
     * @return 插入成功的条数
     */
    int batchInsert(@Param("list") List<SysUserRole> list);
}
