package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.constant.RedisKey;
import cn.pzhdv.blog.dto.article.ArticleCategoryAddDTO;
import cn.pzhdv.blog.dto.article.ArticleCategoryEditDTO;
import cn.pzhdv.blog.entity.ArticleCategory;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.ArticleCategoryService;
import cn.pzhdv.blog.utils.CacheExpireUtil;
import cn.pzhdv.blog.utils.RedisUtils;
import cn.pzhdv.blog.vo.category.CategoryPieChartVO;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * 文章分类表 前端控制器
 *
 * @author PanZonghui
 * @since 2025-06-25 21:03:51
 */
@Slf4j
@Validated
@Api(tags = "文章分类管理")
@RestController
@RequestMapping("/articleCategory")
@RequiredArgsConstructor
public class ArticleCategoryController {

    private final ArticleCategoryService service;
    private final RedisUtils redisUtils;

    @ApiLog("查询分类饼图数据")
    @ApiOperation(
            value = "查询分类饼图数据",
            notes = "查询分类饼图数据",
            httpMethod = "GET",
            produces = "application/json")
    @RequestMapping(value = "pieChart", method = RequestMethod.GET)
    public Result<List<CategoryPieChartVO>> queryArticleCategoryPieChart() {
        List<CategoryPieChartVO> result = service.queryArticleCategoryPieChart();
        return ResultUtil.ok(result);
    }

    @ApiLog("查询分类总数")
    @ApiOperation(
            value = "查询分条总数",
            notes = "查询分类总数",
            httpMethod = "GET",
            produces = "application/json")
    @RequestMapping(value = "total", method = RequestMethod.GET)
    public Result<Long> articleCategoryTotal() {
        // 先从 Redis 中获取缓存数据
        String redisKey = RedisKey.ARTICLE_CATEGORY_TOTAL_KEY;
        Long articleCategoryTotal = redisUtils.get(redisKey, Long.class);
        if (articleCategoryTotal == null) {
            // 如果 Redis 中没有缓存数据，则查询数据库
            articleCategoryTotal = service.count();
            if (articleCategoryTotal > 0) {
                articleCategoryTotal = articleCategoryTotal - 1;// 要 -1，把根节点去掉
            }
            // 将查询结果存入 Redis
            redisUtils.set(redisKey, articleCategoryTotal);
        }
        return ResultUtil.ok(articleCategoryTotal);
    }

    @ApiLog("获取分类树形结构")
    @ApiOperation(
            value = "获取分类树形结构",
            notes = "根据父节点ID查询子节点树形列表，默认查询根节点",
            httpMethod = "GET",
            produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "parentId",
                    value = "父分类ID（默认0=根节点，指定时需≥1）",
                    defaultValue = "0",
                    required = false,
                    dataType = "Long",
                    dataTypeClass = Long.class)
    })
    @GetMapping("listTree")
    public Result<List<ArticleCategory>> getCategoryTreeList(
            @RequestParam(value = "parentId", defaultValue = "0", required = false)
            @Min(value = 0, message = "父分类ID必须≥1（根节点查询请留空或传0）")
            Long parentId) {

        // 构建缓存键
        String redisKey = RedisKey.ARTICLE_CATEGORY_TREE_LIST_KEY + ":parentId=" + parentId;

        // 尝试从缓存获取
        List<ArticleCategory> categoryList = redisUtils.get(redisKey, new TypeReference<>() {
        });
        if (categoryList != null) {
            return ResultUtil.ok(categoryList);
        }

        //  缓存未命中，查询数据库
        categoryList = service.listAllChildren(parentId);

        // 存入缓存（带随机过期时间，避免缓存雪崩）
        int expireSeconds = CacheExpireUtil.getDefaultExpireSeconds();
        redisUtils.set(redisKey, categoryList, expireSeconds);

        return ResultUtil.ok(categoryList);
    }

    @ApiLog("新增文章分类")
    @ApiOperation(
            value = "新增文章分类",
            notes = "添加一条文章分类",
            httpMethod = "POST",
            produces = "application/json")
    @PostMapping("add")
    public Result<Boolean> add(@RequestBody @Validated ArticleCategoryAddDTO addDTO) {

        // DTO转实体
        ArticleCategory articleCategory = new ArticleCategory();
        BeanUtils.copyProperties(addDTO, articleCategory);

        // 执行添加操作
        boolean success = service.save(articleCategory);
        if (!success) {
            log.warn("分类添加失败");
            return ResultUtil.error(ResultCode.ADD_FAIL);
        }

        // 清除相关缓存
        clearCategoryRelatedCache();
        log.info("分类添加成功 | 分类名称：{}，分类ID：{}", articleCategory.getCategoryName(), articleCategory.getCategoryId());
        return ResultUtil.ok(true);
    }

    @ApiLog("编辑文章分类")
    @ApiOperation(
            value = "编辑文章分类",
            notes = "根据ID更新分类信息",
            httpMethod = "PUT",
            produces = "application/json")
    @PutMapping("update")
    public Result<Boolean> update(@RequestBody @Validated ArticleCategoryEditDTO editDTO) {

        // DTO转实体
        ArticleCategory articleCategory = new ArticleCategory();
        BeanUtils.copyProperties(editDTO, articleCategory);

        // 执行更新操作
        boolean success = service.updateById(articleCategory);
        if (!success) {
            log.warn("分类修改失败 | 分类ID: {}", articleCategory.getCategoryId());
            return ResultUtil.error(ResultCode.UPDATE_FAIL);
        }

        // 清除相关缓存
        clearCategoryRelatedCache();
        return ResultUtil.ok(true);
    }

    @ApiLog("删除文章分类")
    @ApiOperation(
            value = "删除文章分类",
            notes = "根据ID删除分类，分类ID必须为正整数",
            httpMethod = "DELETE",
            produces = "application/json")
    @DeleteMapping("delete/{categoryId}")
    public Result<Boolean> deleteCategory(
            @ApiParam(name = "categoryId", value = "分类ID（必须为≥1的正整数）", required = true)
            @PathVariable("categoryId")
            @Min(value = 1, message = "分类ID必须为≥1的正整数")
            Long categoryId) {

        // 执行删除操作（接收Service返回的Map）
        Map<String, Object> deleteResult = service.deleteArticleCategoryById(categoryId);
        boolean isSuccess = (Boolean) deleteResult.get("success");
        String failReason = (String) deleteResult.get("reason");
        if (!isSuccess) {
            // 1. 日志复用Service层已记录的详细信息，此处可简化（避免重复日志）
            log.warn("文章分类删除失败 | 分类ID: {} | 原因：{}", categoryId, failReason);

            // 2. 直接返回Service层定义的失败原因，无需二次校验
            return ResultUtil.error(ResultCode.DELETE_FAIL.getCode(), failReason);
        }

        // 删除成功：清除缓存+返回结果
        clearCategoryRelatedCache();
        log.info("文章分类删除成功 | 分类ID: {} | 原因：{}", categoryId, deleteResult.get("reason"));
        return ResultUtil.ok(true);
    }

    /** 清除分类相关缓存 包含分类自身缓存和关联的文章缓存，确保数据一致性 */
    private void clearCategoryRelatedCache() {
        // 清除分类相关缓存
        redisUtils.del(RedisKey.ARTICLE_CATEGORY_TOTAL_KEY);
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_CATEGORY_LIST_KEY + "*");
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_CATEGORY_TREE_LIST_KEY + "*");

        // 清除关联的文章缓存
        redisUtils.del(RedisKey.ARTICLE_TOTAL_KEY);
        redisUtils.del(RedisKey.ARTICLE_PUBLISH_DATE_LIST_KEY);
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_DETAIL_KEY + "*");
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_HOME_PAGE_LIST_KEY + "*");
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_CATEGORY_PAGE_LIST_KEY + "*");
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_CONDITION_PAGE_LIST_KEY + "*");
    }
}
