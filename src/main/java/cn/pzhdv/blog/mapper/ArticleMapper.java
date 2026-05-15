package cn.pzhdv.blog.mapper;

import cn.pzhdv.blog.entity.Article;
import cn.pzhdv.blog.vo.article.ArticleLineChartVO;
import cn.pzhdv.blog.vo.article.ArticleTotalVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 博客文章表 Mapper 接口
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:24:08
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    List<ArticleLineChartVO> queryArticleLineChart();

    ArticleTotalVO queryArticleTotal();
}
