package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.ArticleCategoryRelation;
import cn.pzhdv.blog.mapper.ArticleCategoryRelationMapper;
import cn.pzhdv.blog.utils.QueryWrapperUtil;
import cn.pzhdv.blog.vo.category.CategoryPieChartVO;
import cn.pzhdv.blog.entity.ArticleCategory;
import cn.pzhdv.blog.mapper.ArticleCategoryMapper;
import cn.pzhdv.blog.service.ArticleCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 文章分类表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:07:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCategoryServiceImpl extends ServiceImpl<ArticleCategoryMapper, ArticleCategory>
        implements ArticleCategoryService {

    private final ArticleCategoryMapper categoryMapper;
    private final ArticleCategoryRelationMapper articleCategoryRelationMapper;

    /**
     * 查询直接子分类
     *
     * @param parentId 父分类ID
     * @return 子分类列表，如果为空则返回空列表
     */
    public List<ArticleCategory> getDirectChildren(Long parentId) {
        QueryWrapper<ArticleCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        List<ArticleCategory> list = categoryMapper.selectList(queryWrapper);
        return list != null ? list : Collections.emptyList();
    }

    /**
     * 递归查询分类及其所有子分类（优化N+1：批量查询+内存组装）
     * <p>
     * 优化说明：
     * 1. 一次查询所有分类数据
     * 2. 在内存中构建父子关系映射
     * 3. 递归构建树形结构
     * </p>
     *
     * @param parentId 父分类ID
     * @return 分类树
     */
    @Override
    public List<ArticleCategory> listAllChildren(Long parentId) {
        // 1. 一次查询所有分类数据
        List<ArticleCategory> allCategories = categoryMapper.selectList(null);
        if (allCategories == null || allCategories.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 构建父子关系映射（parentId -> 子分类列表）
        Map<Integer, List<ArticleCategory>> parentId2ChildrenMap = allCategories.stream()
                .collect(Collectors.groupingBy(
                        ArticleCategory::getParentId,
                        Collectors.toList()
                ));

        // 3. 递归构建树形结构
        return buildCategoryTree(parentId, parentId2ChildrenMap);
    }

    /**
     * 递归构建分类树（内存中组装，无数据库查询）
     *
     * @param parentId               父分类ID
     * @param parentId2ChildrenMap   父子关系映射
     * @return 分类树
     */
    private List<ArticleCategory> buildCategoryTree(Long parentId, Map<Integer, List<ArticleCategory>> parentId2ChildrenMap) {
        List<ArticleCategory> children = parentId2ChildrenMap.get(parentId.intValue());
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }

        for (ArticleCategory child : children) {
            List<ArticleCategory> subChildren = buildCategoryTree(child.getCategoryId(), parentId2ChildrenMap);
            child.setChildren(subChildren);
        }

        return children;
    }

    /**
     * 根据分类ID删除文章分类（含业务校验：禁止删除有子分类/关联文章的分类）
     *
     * @param categoryId 分类ID
     * @return true=删除成功，false=删除失败（存在子分类/关联文章/分类不存在）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteArticleCategoryById(Long categoryId) {
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("success", false);

        // 1. 校验分类是否存在
        ArticleCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            resultMap.put("reason", "分类不存在或已删除");
            log.warn("文章分类删除失败 | 分类ID: {} | 原因：{}", categoryId, resultMap.get("reason"));
            return resultMap;
        }

        // 2. 检查是否有直接子分类
        List<ArticleCategory> directChildren = getDirectChildren(categoryId);
        if (!directChildren.isEmpty()) {
            resultMap.put("reason", "该分类存在子分类，请先删除子分类");
            log.warn("文章分类删除失败 | 分类ID: {} | 子分类数量：{}", categoryId, directChildren.size());
            return resultMap;
        }

        // 3. 检查是否关联文章
        LambdaQueryWrapper<ArticleCategoryRelation> relationWrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaEqCondition(relationWrapper, ArticleCategoryRelation::getCategoryId, categoryId);
        Long count = articleCategoryRelationMapper.selectCount(relationWrapper);
        long relatedArticleCount = count == null ? 0 : count;

        if (relatedArticleCount > 0) {
            resultMap.put("reason", "该分类已关联" + relatedArticleCount + "篇文章，请先解除关联");
            log.warn("文章分类删除失败 | 分类ID: {} | 原因：{}", categoryId, resultMap.get("reason"));
            return resultMap;
        }

        // 4. 删除关联关系
        LambdaQueryWrapper<ArticleCategoryRelation> deleteWrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaEqCondition(deleteWrapper, ArticleCategoryRelation::getCategoryId, categoryId);
        articleCategoryRelationMapper.delete(deleteWrapper);

        // 5. 删除分类
        int deleteRows = categoryMapper.deleteById(categoryId);
        if (deleteRows <= 0) {
            resultMap.put("reason", "删除失败，请稍后重试");
            return resultMap;
        }

        // 6. 成功
        resultMap.put("success", true);
        resultMap.put("reason", "删除成功");
        log.info("文章分类删除成功 | 分类ID: {}", categoryId);
        return resultMap;
    }

    @Override
    public List<CategoryPieChartVO> queryArticleCategoryPieChart() {
        return  categoryMapper.queryArticleCategoryPieChart();
    }
}
