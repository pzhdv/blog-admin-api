package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.BlogMission;
import cn.pzhdv.blog.exception.BusinessException;
import cn.pzhdv.blog.mapper.BlogMissionMapper;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.service.BlogMissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 博客使命表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 19:08:57
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogMissionServiceImpl extends ServiceImpl<BlogMissionMapper, BlogMission> implements BlogMissionService {
    private final BlogMissionMapper blogMissionMapper;

    @Override
    public BlogMission getUniqueMission() {
        // 1. 统计总数（绝不会为null）
        LambdaQueryWrapper<BlogMission> countWrapper = new LambdaQueryWrapper<>();
        long count = blogMissionMapper.selectCount(countWrapper);

        // 2. 数据不唯一 → 抛异常
        if (count > 1) {
            log.error("博客使命数据不唯一，存在{}条数据，请检查数据库", count);
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "博客使命信息不唯一（" + count + "条数据）");
        }

        // 3. 查询唯一数据
        LambdaQueryWrapper<BlogMission> queryWrapper = new LambdaQueryWrapper<>();
        List<BlogMission> missionList = blogMissionMapper.selectList(queryWrapper);

        return missionList.isEmpty() ? null : missionList.get(0);
    }
}
