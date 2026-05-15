package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.BlogMission;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 博客使命表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 19:08:57
 */
public interface BlogMissionService extends IService<BlogMission> {

    BlogMission getUniqueMission();
}
