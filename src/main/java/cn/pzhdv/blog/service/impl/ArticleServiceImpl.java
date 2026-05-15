package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.*;
import cn.pzhdv.blog.mapper.*;
import cn.pzhdv.blog.service.ArticleService;
import cn.pzhdv.blog.utils.QueryWrapperUtil;
import cn.pzhdv.blog.vo.article.ArticleLineChartResult;
import cn.pzhdv.blog.vo.article.ArticleLineChartVO;
import cn.pzhdv.blog.vo.article.ArticleTotalVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {


    private final ArticleMapper articleMapper;

    private final ArticleTagRelationMapper articleTagRelationMapper;

    private final ArticleCategoryRelationMapper articleCategoryRelationMapper;

    private final ArticleCategoryMapper articleCategoryMapper;

    private final ArticleTagMapper articleTagMapper;

    // ==================== 【优化】批量查询，彻底干掉 N+1 ====================
    @Override
    public Page<Article> queryArticleListByConditionPage(
            String title, Boolean publishState, Date startDate, Date endDate,
            String excerptKeyWorld, Integer recommendWeight,
            Integer pageNum, Integer pageSize) {

        // 1. 只查一次文章（主查询）
        Page<Article> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                Article::getArticleId,
                Article::getImage,
                Article::getTitle,
                Article::getExcerpt,
                Article::getPublishState,
                Article::getRecommendWeight,
                Article::getCreateTime,
                Article::getUpdateTime
        );
        queryWrapper.orderByDesc(Article::getUpdateTime);

        // 添加查询条件
        QueryWrapperUtil.addLambdaLikeCondition(queryWrapper, Article::getTitle, title);
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, Article::getPublishState, publishState);
        QueryWrapperUtil.addLambdaGeCondition(queryWrapper, Article::getUpdateTime, startDate);
        QueryWrapperUtil.addLambdaLeCondition(queryWrapper, Article::getUpdateTime, endDate);
        QueryWrapperUtil.addLambdaLikeCondition(queryWrapper, Article::getExcerpt, excerptKeyWorld);
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, Article::getRecommendWeight, recommendWeight);

        Page<Article> resultPage = articleMapper.selectPage(page, queryWrapper);
        List<Article> records = resultPage.getRecords();
        if (records.isEmpty()) return resultPage;

        // ==================== 【关键】批量一次查询所有分类 + 标签 ====================
        List<Long> articleIds = records.stream().map(Article::getArticleId).collect(Collectors.toList());

        // 批量查分类关系
        List<ArticleCategoryRelation> categoryRelations = articleCategoryRelationMapper.selectList(
                new LambdaQueryWrapper<ArticleCategoryRelation>().in(ArticleCategoryRelation::getArticleId, articleIds));
        Map<Long, List<Long>> articleId2CategoryIds = categoryRelations.stream()
                .collect(Collectors.groupingBy(ArticleCategoryRelation::getArticleId,
                        Collectors.mapping(ArticleCategoryRelation::getCategoryId, Collectors.toList())));

        // 批量查标签关系
        List<ArticleTagRelation> tagRelations = articleTagRelationMapper.selectList(
                new LambdaQueryWrapper<ArticleTagRelation>().in(ArticleTagRelation::getArticleId, articleIds));
        Map<Long, List<Long>> articleId2TagIds = tagRelations.stream()
                .collect(Collectors.groupingBy(ArticleTagRelation::getArticleId,
                        Collectors.mapping(ArticleTagRelation::getArticleTagId, Collectors.toList())));

        // 批量查 分类实体
        Set<Long> allCategoryIds = categoryRelations.stream().map(ArticleCategoryRelation::getCategoryId).collect(Collectors.toSet());
        Map<Long, ArticleCategory> categoryMap = allCategoryIds.isEmpty() ? Collections.emptyMap() :
                articleCategoryMapper.selectByIds(allCategoryIds).stream()
                        .collect(Collectors.toMap(ArticleCategory::getCategoryId, c -> c));

        // 批量查 标签实体
        Set<Long> allTagIds = tagRelations.stream().map(ArticleTagRelation::getArticleTagId).collect(Collectors.toSet());
        Map<Long, ArticleTag> tagMap = allTagIds.isEmpty() ? Collections.emptyMap() :
                articleTagMapper.selectByIds(allTagIds).stream()
                        .collect(Collectors.toMap(ArticleTag::getArticleTagId, t -> t));

        // ==================== 内存组装（0 SQL） ====================
        for (Article article : records) {
            // 分类
            List<Long> cIds = articleId2CategoryIds.getOrDefault(article.getArticleId(), Collections.emptyList());
            List<ArticleCategory> cList = cIds.stream().map(categoryMap::get).filter(Objects::nonNull).collect(Collectors.toList());
            article.setCategoryIds(cIds);
            article.setArticleCategoryList(cList);

            // 标签
            List<Long> tIds = articleId2TagIds.getOrDefault(article.getArticleId(), Collections.emptyList());
            List<ArticleTag> tList = tIds.stream().map(tagMap::get).filter(Objects::nonNull).collect(Collectors.toList());
            article.setTagIds(tIds);
            article.setArticleTagList(tList);
        }

        return resultPage;
    }

    // ==================== 以下方法保持不变，只优化了分页 ====================

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteArticleById(Long articleId) {
        int rows = articleMapper.deleteById(articleId);
        if (rows > 0) {
            deleteArticleRelations(articleId);
            return true;
        }
        log.warn("删除文章失败，文章不存在：{}", articleId);
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteBatchArticleByIds(List<Long> articleIdList) {
        if (articleIdList == null || articleIdList.isEmpty()) {
            log.warn("批量删除文章失败，id列表为空");
            return false;
        }
        articleCategoryRelationMapper.delete(new LambdaQueryWrapper<ArticleCategoryRelation>().in(ArticleCategoryRelation::getArticleId, articleIdList));
        articleTagRelationMapper.delete(new LambdaQueryWrapper<ArticleTagRelation>().in(ArticleTagRelation::getArticleId, articleIdList));
        return this.removeByIds(articleIdList);
    }

    @Override
    public ArticleLineChartResult queryArticleLineChart() {
        List<ArticleLineChartVO> list = articleMapper.queryArticleLineChart();
        ArticleLineChartResult result = new ArticleLineChartResult();
        List<String> ymList = new ArrayList<>();
        List<Long> publishedCntList = new ArrayList<>();
        List<Long> draftCntList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            for (ArticleLineChartVO item : list) {
                ymList.add(item.getYm());
                publishedCntList.add(item.getPublishedCnt());
                draftCntList.add(item.getDraftCnt());
            }
        }
        result.setYmList(ymList);
        result.setPublishedCntList(publishedCntList);
        result.setDraftCntList(draftCntList);
        return result;
    }

    @Override
    public ArticleTotalVO queryArticleTotal() {
        return articleMapper.queryArticleTotal();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateArticle(Article article) {
        Long articleId = article.getArticleId();
        Article existArticle = articleMapper.selectById(articleId);
        if (existArticle == null) {
            log.warn("修改文章失败，文章不存在：{}", articleId);
            return false;
        }
        article.setCreateTime(existArticle.getCreateTime());
        int rows = articleMapper.updateById(article);
        deleteArticleRelations(articleId);
        saveArticleRelations(articleId, article.getCategoryIds(), article.getTagIds());
        return rows > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveArticle(Article article) {
        int rows = articleMapper.insert(article);
        if (rows <= 0) {
            log.error("文章插入失败");
            return false;
        }
        Long articleId = article.getArticleId();
        saveArticleRelations(articleId, article.getCategoryIds(), article.getTagIds());
        return true;
    }

    @Override
    public Article queryArticleById(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) return null;
        setArticleCategories(article);
        setArticleTags(article);
        return article;
    }

    private void saveArticleRelations(Long articleId, List<Long> categoryIds, List<Long> tagIds) {
        // 批量插入：分类关系
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<ArticleCategoryRelation> categoryRelations = new ArrayList<>();
            for (Long categoryId : categoryIds) {
                ArticleCategoryRelation relation = new ArticleCategoryRelation();
                relation.setArticleId(articleId);
                relation.setCategoryId(categoryId);
                categoryRelations.add(relation);
            }
            // 批量插入 1 次，而非循环插入
            articleCategoryRelationMapper.insert(categoryRelations);
        }

        // 批量插入：标签关系
        if (tagIds != null && !tagIds.isEmpty()) {
            List<ArticleTagRelation> tagRelations = new ArrayList<>();
            for (Long tagId : tagIds) {
                ArticleTagRelation relation = new ArticleTagRelation();
                relation.setArticleId(articleId);
                relation.setArticleTagId(tagId);
                tagRelations.add(relation);
            }
            // 批量插入 1 次
            articleTagRelationMapper.insert(tagRelations);
        }
    }

    private void deleteArticleRelations(Long articleId) {
        articleTagRelationMapper.delete(new LambdaQueryWrapper<ArticleTagRelation>().eq(ArticleTagRelation::getArticleId, articleId));
        articleCategoryRelationMapper.delete(new LambdaQueryWrapper<ArticleCategoryRelation>().eq(ArticleCategoryRelation::getArticleId, articleId));
    }

    private void setArticleCategories(Article article) {
        Long articleId = article.getArticleId();
        List<ArticleCategoryRelation> relations = articleCategoryRelationMapper.selectList(
                new LambdaQueryWrapper<ArticleCategoryRelation>().eq(ArticleCategoryRelation::getArticleId, articleId));
        if (relations == null || relations.isEmpty()) {
            article.setCategoryIds(new ArrayList<>());
            article.setArticleCategoryList(new ArrayList<>());
            return;
        }
        List<Long> categoryIds = relations.stream().map(ArticleCategoryRelation::getCategoryId).collect(Collectors.toList());
        List<ArticleCategory> categoryList = articleCategoryMapper.selectByIds(categoryIds);
        article.setCategoryIds(categoryIds);
        article.setArticleCategoryList(categoryList);
    }

    private void setArticleTags(Article article) {
        Long articleId = article.getArticleId();
        List<ArticleTagRelation> relations = articleTagRelationMapper.selectList(
                new LambdaQueryWrapper<ArticleTagRelation>().eq(ArticleTagRelation::getArticleId, articleId));
        if (relations == null || relations.isEmpty()) {
            article.setTagIds(new ArrayList<>());
            article.setArticleTagList(new ArrayList<>());
            return;
        }
        List<Long> tagIds = relations.stream().map(ArticleTagRelation::getArticleTagId).collect(Collectors.toList());
        List<ArticleTag> tagList = articleTagMapper.selectByIds(tagIds);
        article.setTagIds(tagIds);
        article.setArticleTagList(tagList);
    }
}