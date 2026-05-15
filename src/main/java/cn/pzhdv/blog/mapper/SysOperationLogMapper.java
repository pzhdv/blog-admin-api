package cn.pzhdv.blog.mapper;

import cn.pzhdv.blog.entity.SysOperationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 系统接口访问日志表 Mapper 接口
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-13 17:46:48
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

}
