package cn.pzhdv.blog.mapper;

import cn.pzhdv.blog.entity.ArticleCategoryRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 文章-分类关联表 Mapper 接口
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:24:08
 */
@Mapper
public interface ArticleCategoryRelationMapper extends BaseMapper<ArticleCategoryRelation> {

}
