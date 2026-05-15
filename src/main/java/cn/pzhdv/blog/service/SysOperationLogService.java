package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.SysOperationLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 系统接口访问日志表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-13 17:46:48
 */

public interface SysOperationLogService extends IService<SysOperationLog> {

    /**
     * 异步保存操作日志
     * @param logEntity 日志实体
     */
    void saveLogAsync(SysOperationLog logEntity);
}
