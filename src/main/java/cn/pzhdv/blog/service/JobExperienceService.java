package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.JobExperience;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 工作经历表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 19:08:57
 */
public interface JobExperienceService extends IService<JobExperience> {

    List<JobExperience> queryJobExperienceList();
}
