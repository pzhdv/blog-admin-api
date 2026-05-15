package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.SysOperationLog;
import cn.pzhdv.blog.mapper.SysOperationLogMapper;
import cn.pzhdv.blog.service.SysOperationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统接口访问日志表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-13 17:46:48
 */
@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog> implements SysOperationLogService {

    private final SysOperationLogMapper operationLogMapper;

    @Override
    @Async
    public void saveLogAsync(SysOperationLog logEntity) {
        try {
            operationLogMapper.insert(logEntity);
        } catch (Exception e) {
            // 异步任务内部需捕获异常，防止影响主链路
        }
    }
}
