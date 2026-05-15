package cn.pzhdv.blog.mapper;

import cn.pzhdv.blog.entity.ArticleCategory;
import cn.pzhdv.blog.vo.category.CategoryPieChartVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 文章分类表 Mapper 接口
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:07:36
 */
@Mapper
public interface ArticleCategoryMapper extends BaseMapper<ArticleCategory> {

    List<CategoryPieChartVO> queryArticleCategoryPieChart();
}

