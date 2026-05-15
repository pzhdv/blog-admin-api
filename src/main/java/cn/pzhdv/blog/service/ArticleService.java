package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.Article;
import cn.pzhdv.blog.vo.article.ArticleLineChartResult;
import cn.pzhdv.blog.vo.article.ArticleTotalVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 博客文章表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:24:08
 */
public interface ArticleService extends IService<Article> {

    ArticleTotalVO queryArticleTotal();

    ArticleLineChartResult queryArticleLineChart();

    Page<Article> queryArticleListByConditionPage(String title, Boolean publishState, Date startDate, Date endDate, String excerptKeyWorld, Integer recommendWeight, @Min(value = 1, message = "页码必须≥1") Integer current, @Min(value = 1, message = "每页条数必须≥1") @Max(value = 100, message = "每页条数不能超过100") Integer size);

    Article queryArticleById(@Min(value = 1, message = "文章ID必须≥1") Long articleId);

    boolean saveArticle(Article article);

    boolean updateArticle(Article article);

    boolean deleteArticleById(@Min(value = 1, message = "文章ID必须≥1") Long articleId);

    boolean deleteBatchArticleByIds(List<Long> articleIdList);
}
