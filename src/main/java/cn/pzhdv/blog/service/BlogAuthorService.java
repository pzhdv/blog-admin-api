package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.BlogAuthor;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户个人信息表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 19:08:57
 */
public interface BlogAuthorService extends IService<BlogAuthor> {

    BlogAuthor getUniqueAuthor();
}
