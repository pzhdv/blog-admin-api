package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.JobExperience;
import cn.pzhdv.blog.mapper.JobExperienceMapper;
import cn.pzhdv.blog.service.JobExperienceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 工作经历表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 19:08:57
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExperienceServiceImpl extends ServiceImpl<JobExperienceMapper, JobExperience> implements JobExperienceService {

    private final JobExperienceMapper jobExperienceMapper;

    @Override
    public List<JobExperience> queryJobExperienceList() {
        LambdaQueryWrapper<JobExperience> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(JobExperience::getId);
        return jobExperienceMapper.selectList(wrapper);
    }
}
