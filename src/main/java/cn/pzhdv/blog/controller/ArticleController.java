package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.constant.RedisKey;
import cn.pzhdv.blog.dto.article.ArticleAddDTO;
import cn.pzhdv.blog.dto.article.ArticleEditDTO;
import cn.pzhdv.blog.dto.article.DraftArticle;
import cn.pzhdv.blog.dto.common.BatchDeleteReq;
import cn.pzhdv.blog.entity.Article;
import cn.pzhdv.blog.vo.article.ArticleLineChartResult;
import cn.pzhdv.blog.vo.article.ArticleTotalVO;
import cn.pzhdv.blog.exception.BusinessException;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.ArticleService;
import cn.pzhdv.blog.utils.CacheExpireUtil;
import cn.pzhdv.blog.utils.RedisUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 博客文章表 前端控制器
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:24:08
 */
@Slf4j
@Validated
@Api(tags = "文章管理")
@RestController
@RequestMapping("/article")
@RequiredArgsConstructor
public class ArticleController {

    private final RedisUtils redisUtils;
    private final ArticleService baseService;

    @ApiLog("查询文章总数")
    @ApiOperation(
            value = "查询文章总数",
            notes = "文章总数(3)、已发布(2)、草稿(1)",
            httpMethod = "GET",
            produces = "application/json")
    @GetMapping("total")
    public Result<ArticleTotalVO> queryArticleTotal() {
        ArticleTotalVO totalVO = baseService.queryArticleTotal();
        return ResultUtil.ok(totalVO);
    }

    @ApiLog("查询文章折线图数据")
    @ApiOperation(
            value = "查询文章折线图数据",
            notes = "年月、发布数量、草稿数量",
            httpMethod = "GET",
            produces = "application/json")
    @GetMapping("lineChart")
    public Result<ArticleLineChartResult> queryArticleLineChart() {
        ArticleLineChartResult list = baseService.queryArticleLineChart();
        return ResultUtil.ok(list);
    }

    @ApiLog("文章条件分页查询")
    @ApiOperation(
            value = "文章条件分页查询",
            notes = "支持标题、发布状态、日期范围等条件，默认分页10条/页",
            httpMethod = "GET",
            produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "title",
                    value = "文章标题（模糊匹配）",
                    paramType = "query",
                    dataType = "String",
                    dataTypeClass = String.class),
            @ApiImplicitParam(
                    name = "publishState",
                    value = "发布状态（true=已发布，false=草稿）",
                    paramType = "query",
                    dataType = "Boolean",
                    dataTypeClass = Boolean.class),
            @ApiImplicitParam(
                    name = "startDate",
                    value = "开始日期（格式：yyyy-MM-dd）",
                    paramType = "query",
                    dataType = "Date",
                    dataTypeClass = Date.class),
            @ApiImplicitParam(
                    name = "endDate",
                    value = "结束日期（格式：yyyy-MM-dd）",
                    paramType = "query",
                    dataType = "Date",
                    dataTypeClass = Date.class),
            @ApiImplicitParam(
                    name = "excerptKeyWorld",
                    value = "摘要关键字（模糊匹配）",
                    paramType = "query",
                    dataType = "String",
                    dataTypeClass = String.class),
            @ApiImplicitParam(
                    name = "recommendWeight",
                    value = "推荐权重（精确匹配，≥0）",
                    paramType = "query",
                    dataType = "Integer",
                    dataTypeClass = Integer.class),
            @ApiImplicitParam(
                    name = "current",
                    value = "当前页码（≥1）",
                    paramType = "query",
                    required = true,
                    dataType = "Integer",
                    dataTypeClass = Integer.class,
                    defaultValue = "1"),
            @ApiImplicitParam(
                    name = "size",
                    value = "每页条数（1-100）",
                    paramType = "query",
                    required = true,
                    dataType = "Integer",
                    dataTypeClass = Integer.class,
                    defaultValue = "10")
    })
    @GetMapping("conditionPageList")
    public Result<Page<Article>> queryArticleListByConditionPage(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "publishState", required = false) Boolean publishState,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date endDate,
            @RequestParam(value = "excerptKeyWorld", required = false) String excerptKeyWorld,
            @RequestParam(value = "recommendWeight", required = false) Integer recommendWeight,
            @RequestParam(value = "current", defaultValue = "1") @Min(value = 1, message = "页码必须≥1")
            Integer current,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数必须≥1")
            Integer size) {

        // 1. 日期逻辑校验：开始日期不能晚于结束日期
        if (startDate != null && endDate != null && startDate.after(endDate)) {
            log.warn("文章条件查询失败 | 开始日期晚于结束日期 | startDate={}, endDate={}", startDate, endDate);
            return ResultUtil.error(ResultCode.PARAM_INVALID, "开始日期不能晚于结束日期");
        }

        // 2. 推荐权重校验（若传值则必须≥0）
        if (recommendWeight != null && recommendWeight < 0) {
            log.warn("文章条件查询失败 | 推荐权重为负数 | recommendWeight={}", recommendWeight);
            return ResultUtil.error(ResultCode.PARAM_INVALID, "推荐权重不能为负数");
        }

        // 3. 构建缓存键（处理null值，避免缓存键包含"null"）
        String redisKey =
                buildConditionCacheKey(
                        title,
                        publishState,
                        startDate,
                        endDate,
                        excerptKeyWorld,
                        recommendWeight,
                        current,
                        size);

        // 4. 从缓存获取数据
        Page<Article> articlePage = redisUtils.get(redisKey, new TypeReference<Page<Article>>() {
        });
        if (articlePage != null) {
            log.debug("文章条件查询命中缓存 | key={}", redisKey);
            return ResultUtil.ok(articlePage);
        }

        // 5. 缓存未命中，查询数据库
        articlePage =
                baseService.queryArticleListByConditionPage(
                        title,
                        publishState,
                        startDate,
                        endDate,
                        excerptKeyWorld,
                        recommendWeight,
                        current,
                        size);
        if (articlePage == null) {
            log.error("文章条件查询数据库返回null | 条件：title={}, current={}", title, current);
            return ResultUtil.error(ResultCode.SERVER_ERROR, "查询失败，请重试");
        }

        // 6. 存入缓存
        int expireSeconds = CacheExpireUtil.getDefaultExpireSeconds();
        redisUtils.set(redisKey, articlePage, expireSeconds);
        log.debug("文章条件查询结果存入缓存 | key={}, 过期时间={}s", redisKey, expireSeconds);

        return ResultUtil.ok(articlePage);
    }

    @ApiLog("查询文章详情")
    @ApiOperation(
            value = "查询文章详情",
            notes = "根据文章ID查询完整详情，包含分类和标签信息",
            httpMethod = "GET",
            produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "articleId",
                    value = "文章ID（≥1）",
                    paramType = "query",
                    required = true,
                    dataType = "Long",
                    dataTypeClass = Long.class)
    })
    @GetMapping("articleDetailById")
    public Result<Article> queryArticleDetailById(
            @RequestParam(value = "articleId", required = true) @Min(value = 1, message = "文章ID必须≥1")
            Long articleId) {

        // 1. 构建缓存键
        String redisKey = RedisKey.ARTICLE_DETAIL_MARKDOWN_KEY + articleId;

        // 2. 从缓存获取
        Article article = redisUtils.get(redisKey, Article.class);
        if (article != null) {
            log.debug("文章详情命中缓存 | id={}, key={}", articleId, redisKey);
            return ResultUtil.ok(article);
        }

        // 3. 缓存未命中，查询数据库
        article = baseService.queryArticleById(articleId);
        if (article == null) {
            log.warn("文章详情查询失败 | 文章不存在 | id={}", articleId);
            throw new BusinessException(ResultCode.RESOURCE_DELETED, "文章不存在或已删除");
        }

        // 4. 存入缓存
        int expireSeconds = CacheExpireUtil.getDefaultExpireSeconds();
        redisUtils.set(redisKey, article, expireSeconds);
        log.debug("文章详情存入缓存 | id={}, key={}, 过期时间={}s", articleId, redisKey, expireSeconds);

        return ResultUtil.ok(article);
    }

    @ApiLog("保存草稿")
    @ApiOperation(
            value = "保存草稿",
            notes = "保存文章为草稿",
            httpMethod = "POST",
            produces = "application/json")
    @PostMapping("saveDraft")
    public Result<Boolean> saveDraft(@RequestBody @Validated DraftArticle draftArticle) {
        Article article;
        // 若有articleId（从已发布文章编辑），先查询旧数据
        if (draftArticle.getArticleId() != null) {
            article = baseService.getById(draftArticle.getArticleId());
            if (article == null) {
                log.error("保存草稿失败 | 文章不存在 id={}", draftArticle.getArticleId());
                return ResultUtil.error(ResultCode.DRAFT_FAIL);
            }
        } else {
            // 无ID则为新草稿，新建对象
            article = new Article();
            article.setRecommendWeight(1);
        }

        // 覆盖本次编辑的字段（核心内容）
        article.setTitle(draftArticle.getTitle());
        article.setMarkdown(draftArticle.getMarkdown());
        article.setPublishState(false); // 强制设为草稿

        // 2. 执行新增操作
        boolean success = baseService.saveOrUpdate(article);
        if (!success) {
            log.error("保存草稿失败  title={}", article.getTitle());
            return ResultUtil.error(ResultCode.DRAFT_FAIL);
        }
        // 3. 清除相关缓存
        clearArticleRelatedCache();
        log.info("保存草稿成功 | id={}, title={}", article.getArticleId(), article.getTitle());

        return ResultUtil.ok(true);
    }

    @ApiLog("新增文章")
    @ApiOperation(
            value = "新增文章",
            notes = "支持关联分类和标签，自动填充创建时间，标题不可重复",
            httpMethod = "POST",
            produces = "application/json")
    @PostMapping("add")
    public Result<Boolean> addArticle(@RequestBody @Valid ArticleAddDTO articleAddDTO) {
        // 1. 请求体非空校验
        if (articleAddDTO == null) {
            log.warn("新增文章失败 | 请求体为空");
            return ResultUtil.error(ResultCode.PARAM_INVALID, "请传递文章数据");
        }

        // 2. DTO转实体
        Article article = new Article();
        BeanUtils.copyProperties(articleAddDTO, article);

        // 3. 执行新增操作
        boolean success = baseService.saveArticle(article);
        if (!success) {
            log.error("新增文章失败 | 标题可能重复 | title={}", article.getTitle());
            return ResultUtil.error(ResultCode.ADD_FAIL, "新增失败，标题可能已存在");
        }

        // 4. 清除相关缓存
        clearArticleRelatedCache();
        log.info("新增文章成功 | id={}, title={}", article.getArticleId(), article.getTitle());

        return ResultUtil.ok(true);
    }

    @ApiLog("编辑文章")
    @ApiOperation(
            value = "编辑文章",
            notes = "根据文章ID更新文章内容，支持修改标题、正文、摘要、发布状态等字段，标题修改时会校验唯一性",
            httpMethod = "PUT",
            produces = "application/json")
    @PutMapping("update")
    public Result<Boolean> updateArticle(@RequestBody @Valid ArticleEditDTO articleEditDTO) {
        // 1. 请求体及ID非空校验
        if (articleEditDTO == null || articleEditDTO.getArticleId() == null) {
            log.warn("修改文章失败 | 请求体为空或文章ID为null");
            return ResultUtil.error(ResultCode.PARAM_REQUIRED, "文章ID不能为空");
        }

        // 2. DTO转实体
        Article article = new Article();
        BeanUtils.copyProperties(articleEditDTO, article);

        // 3. 执行修改操作
        boolean success = baseService.updateArticle(article);
        if (!success) {
            log.error("修改文章失败 | 文章不存在或标题重复 | id={}, title={}", article.getArticleId(), article.getTitle());
            return ResultUtil.error(ResultCode.UPDATE_FAIL, "修改失败，文章不存在或标题重复");
        }

        // 4. 清除相关缓存
        clearArticleRelatedCache();
        log.info("修改文章成功 | id={}", article.getArticleId());

        return ResultUtil.ok(true);
    }

    @ApiLog("删除文章")
    @ApiOperation(
            value = "删除文章",
            notes = "根据文章ID删除，支持关联标签和分类的关联关系清理",
            httpMethod = "DELETE",
            produces = "application/json")
    @DeleteMapping("delete/{articleId}")
    public Result<Boolean> deleteArticleById(
            @ApiParam(name = "articleId", value = "文章ID（≥1）", required = true)
            @PathVariable("articleId")
            @Min(value = 1, message = "文章ID必须≥1")
            Long articleId) {

        // 1. 先校验文章是否存在（避免无效删除）
        Article article = baseService.getById(articleId);
        if (article == null) {
            log.warn("删除文章失败 | 文章不存在 | id={}", articleId);
            return ResultUtil.error(ResultCode.RESOURCE_DELETED, "文章不存在或已删除");
        }

        // 2. 执行删除（事务 + 清理关联关系）
        boolean success = baseService.deleteArticleById(articleId);
        if (!success) {
            log.error("删除文章失败 | 可能存在关联数据 | id={}", articleId);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "删除失败，可能存在关联的标签或分类");
        }

        // 3. 清除相关缓存
        clearArticleRelatedCache();
        log.info("删除文章成功 | id={}", articleId);

        return ResultUtil.ok(true);
    }


    @ApiLog("批量删除文章")
    @ApiOperation(
            value = "批量删除文章",
            notes = "批量删除文章（谨慎操作，ID≥1），自动清理关联关系与缓存",
            httpMethod = "DELETE",
            produces = "application/json")
    @DeleteMapping("delete/batch")
    public Result<Boolean> deleteBatch(
            @RequestBody @Valid
            BatchDeleteReq deleteReq) {

        // 1. 获取文章ID列表
        List<Long> articleIdList = deleteReq.getIds();

        // 2. 空列表校验
        if (articleIdList.isEmpty()) {
            log.warn("批量删除文章失败 | 待删除文章ID列表为空");
            return ResultUtil.error(ResultCode.DELETE_FAIL, "待删除文章ID列表不能为空");
        }

        // 3. 批量校验文章是否存在
        List<Article> existArticles = baseService.listByIds(articleIdList);
        List<Long> existArticleIdList = existArticles.stream()
                .map(Article::getArticleId)
                .toList();

        // 4. 筛选不存在的文章ID
        List<Long> notExistIds = articleIdList.stream()
                .filter(id -> !existArticleIdList.contains(id))
                .toList();

        // 5. 有任何一个不存在，直接抛出异常（保持原子性）
        if (!notExistIds.isEmpty()) {
            log.warn("批量删除文章失败 | 部分文章不存在 | 不存在ID：{}", notExistIds);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND,
                    String.format("以下文章不存在：%s", notExistIds));
        }

        // 6. 执行批量删除
        boolean success = baseService.deleteBatchArticleByIds(articleIdList);
        if (!success) {
            log.error("批量删除文章失败 | 数据库操作异常 | ID列表：{}", articleIdList);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "删除失败，数据库操作异常");
        }

        // 7. 清除缓存
        clearArticleRelatedCache();

        // 8. 日志
        log.info("批量删除文章成功 | 数量：{} | ID列表：{}", articleIdList.size(), articleIdList);

        return ResultUtil.ok(true);
    }

    /**
     * 构建条件分页查询的缓存键（处理null值，避免缓存键异常）
     */
    private String buildConditionCacheKey(
            String title,
            Boolean publishState,
            Date startDate,
            Date endDate,
            String excerptKeyWorld,
            Integer recommendWeight,
            Integer current,
            Integer size) {
        // 用空字符串替换null，避免缓存键包含"null"；trim()处理空格，避免重复缓存
        String titleStr = Objects.isNull(title) ? "" : title.trim();
        String publishStateStr = Objects.isNull(publishState) ? "" : publishState.toString();
        String startDateStr = Objects.isNull(startDate) ? "" : startDate.toString();
        String endDateStr = Objects.isNull(endDate) ? "" : endDate.toString();
        String excerptKeyWorldStr = Objects.isNull(excerptKeyWorld) ? "" : excerptKeyWorld.trim();
        String recommendWeightStr = Objects.isNull(recommendWeight) ? "" : recommendWeight.toString();

        return RedisKey.ARTICLE_CONDITION_PAGE_LIST_KEY
                + ":title="
                + titleStr
                + ":publishState="
                + publishStateStr
                + ":startDate="
                + startDateStr
                + ":endDate="
                + endDateStr
                + ":excerptKeyWorld="
                + excerptKeyWorldStr
                + ":recommendWeight="
                + recommendWeightStr
                + ":current="
                + current
                + ":size="
                + size;
    }

    /**
     * 清除文章相关的所有缓存（统一管理，减少重复代码）
     */
    private void clearArticleRelatedCache() {
        // 文章总数缓存
        redisUtils.del(RedisKey.ARTICLE_TOTAL_KEY);
        // 发布日期列表缓存
        redisUtils.del(RedisKey.ARTICLE_PUBLISH_DATE_LIST_KEY);
        // 文章详情缓存 markdown（所有）
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_DETAIL_MARKDOWN_KEY + "*");
        // 文章详情缓存 html（所有）
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_DETAIL_HTML_KEY + "*");
        // 首页文章列表缓存
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_HOME_PAGE_LIST_KEY + "*");
        // 分类文章列表缓存
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_CATEGORY_PAGE_LIST_KEY + "*");
        // 条件分页查询缓存
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_CONDITION_PAGE_LIST_KEY + "*");
        // 移动端分类统计缓存
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_CATEGORY_LIST_KEY + "*");
    }
}

