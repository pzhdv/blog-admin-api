package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.ArticleTag;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.constraints.Min;
import java.util.Map;

/**
 * <p>
 * 文章标签表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:07:36
 */
public interface ArticleTagService extends IService<ArticleTag> {

    Map<String, Object> deleteArticleTagById(Long articleTagId);
}
