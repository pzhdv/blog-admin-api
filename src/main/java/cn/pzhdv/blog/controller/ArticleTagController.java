package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.constant.RedisKey;
import cn.pzhdv.blog.dto.article.ArticleTagAddDTO;
import cn.pzhdv.blog.dto.article.ArticleTagEditDTO;
import cn.pzhdv.blog.entity.ArticleTag;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.ArticleTagService;
import cn.pzhdv.blog.utils.CacheExpireUtil;
import cn.pzhdv.blog.utils.RedisUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章标签表 前端控制器
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:07:36
 */
@Slf4j
@Validated
@Api(tags = "文章标签管理")
@RestController
@RequestMapping("/articleTag")
@RequiredArgsConstructor
public class ArticleTagController {

    private final RedisUtils redisUtils;
    private final ArticleTagService service;

    @ApiLog("查询标签总数")
    @ApiOperation(
            value = "查询标签总数",
            notes = "查询标签总数",
            httpMethod = "GET",
            produces = "application/json")
    @RequestMapping(value = "total", method = RequestMethod.GET)
    public Result<Long> articleTagTotal() {
        String redisKey = RedisKey.ARTICLE_TAG_TOTAL_KEY;
        Long articleTagTotal = redisUtils.get(redisKey, Long.class);
        if (articleTagTotal == null) {
            articleTagTotal = service.count();
            int expireSeconds = CacheExpireUtil.getDefaultExpireSeconds();
            redisUtils.set(redisKey, articleTagTotal, expireSeconds);
        }
        return ResultUtil.ok(articleTagTotal);
    }

    @ApiLog("查询所有标签列表")
    @ApiOperation(
            value = "查询所有标签列表",
            notes = "获取系统中所有文章标签（含缓存，用于前端下拉选择/标签展示）",
            httpMethod = "GET",
            produces = "application/json")
    @GetMapping("list")
    public Result<List<ArticleTag>> listAllTags() {
        String cacheKey = RedisKey.ARTICLE_TAG_CACHE_KEY;
        List<ArticleTag> tagList = redisUtils.get(cacheKey, new TypeReference<List<ArticleTag>>() {
        });

        if (tagList != null) {
            return ResultUtil.ok(tagList);
        }

        // 缓存未命中，查询数据库
        log.debug("标签缓存未命中，从数据库查询 | key: {}", cacheKey);
        tagList = service.list();
        if (tagList != null) {
            // 存入缓存
            int expireSeconds = CacheExpireUtil.getDefaultExpireSeconds();
            redisUtils.set(cacheKey, tagList, expireSeconds);
            log.info(
                    "标签数据写入缓存成功 | key: {}, 过期时间: {}秒, 标签数量: {}", cacheKey, expireSeconds, tagList.size());
        }

        return ResultUtil.ok(tagList);
    }

    @ApiLog("新增文章标签")
    @ApiOperation(
            value = "新增文章标签",
            notes = "新增标签（名称不可重复，1-30个字符）",
            httpMethod = "POST",
            produces = "application/json")
    @PostMapping("add")
    public Result<Boolean> addTag(@RequestBody @Validated ArticleTagAddDTO addDTO) {

        String tagName = addDTO.getArticleTagName();

        // DTO转实体
        ArticleTag articleTag = new ArticleTag();
        BeanUtils.copyProperties(addDTO, articleTag);

        // 执行添加
        boolean success = service.save(articleTag);
        if (!success) {
            log.error("标签添加失败 | 可能原因：数据库异常 | 标签名称: {}", tagName);
            return ResultUtil.error(ResultCode.ADD_FAIL);
        }

        // 清除缓存（确保新增后前端能获取最新标签列表）
        redisUtils.del(RedisKey.ARTICLE_TAG_CACHE_KEY);
        redisUtils.del(RedisKey.ARTICLE_TAG_TOTAL_KEY);
        clearRelatedArticleCache(); // 同步清除文章关联缓存
        log.info("标签添加成功并清除缓存 | 标签ID: {}, 标签名称: {}", articleTag.getArticleTagId(), tagName);

        return ResultUtil.ok(true);
    }

    @ApiLog("编辑文章标签")
    @ApiOperation(
            value = "编辑文章标签",
            notes = "更新标签信息（ID必传，名称不可重复）",
            httpMethod = "PUT",
            produces = "application/json")
    @PutMapping("update")
    public Result<Boolean> updateTag(@RequestBody @Validated ArticleTagEditDTO editDTO) {
        Long tagId = editDTO.getArticleTagId();
        String tagName = editDTO.getArticleTagName();

        // DTO转实体
        ArticleTag articleTag = new ArticleTag();
        BeanUtils.copyProperties(editDTO, articleTag);

        // 执行修改
        boolean success = service.updateById(articleTag);
        if (!success) {
            log.error("标签修改失败 | 可能原因：标签不存在/名称重复 | 标签ID: {}, 标签名称: {}", tagId, tagName);
            return ResultUtil.error(ResultCode.UPDATE_FAIL);
        }

        // 清除关联缓存（标签变化影响文章展示）
        redisUtils.del(RedisKey.ARTICLE_TAG_CACHE_KEY);
        redisUtils.del(RedisKey.ARTICLE_TAG_TOTAL_KEY);
        clearRelatedArticleCache();
        log.info("标签修改成功并清除缓存 | 标签ID: {}, 原名称→新名称: （略）→{}", tagId, tagName);

        return ResultUtil.ok(true);
    }


    @ApiLog("删除文章标签")
    @ApiOperation(
            value = "删除文章标签",
            notes = "根据标签ID删除，若标签已关联文章则删除失败",
            httpMethod = "DELETE",
            produces = "application/json")
    @DeleteMapping("delete/{articleTagId}")
    public Result<Boolean> deleteTag(
            @PathVariable @ApiParam(name = "articleTagId", value = "标签ID（≥1）", required = true)
            @Min(value = 1, message = "标签ID必须≥1")
            Long articleTagId) {
        // 执行删除（Service层需校验：标签是否关联文章，关联则返回false）
        Map<String, Object> deleteResult = service.deleteArticleTagById(articleTagId);
        boolean isSuccess = (Boolean) deleteResult.get("success");
        String failReason = (String) deleteResult.get("reason");
        if (!isSuccess) {
            // 1. 日志复用Service层已记录的详细信息，此处可简化（避免重复日志）
            log.warn("标签删除失败 | 分类ID: {} | 原因：{}", articleTagId, failReason);

            // 2. 直接返回Service层定义的失败原因，无需二次校验
            return ResultUtil.error(ResultCode.DELETE_FAIL.getCode(), failReason);
        }

        // 清除关联缓存
        redisUtils.del(RedisKey.ARTICLE_TAG_CACHE_KEY);
        redisUtils.del(RedisKey.ARTICLE_TAG_TOTAL_KEY);
        clearRelatedArticleCache();
        log.info("标签删除成功 | 分类ID: {} | 原因：{}", articleTagId, deleteResult.get("reason"));

        return ResultUtil.ok(true);
    }

    /**
     * 清除标签关联的文章缓存（标签变化会影响文章列表/详情的标签展示）
     */
    private void clearRelatedArticleCache() {
        redisUtils.del(RedisKey.ARTICLE_TOTAL_KEY);
        redisUtils.del(RedisKey.ARTICLE_PUBLISH_DATE_LIST_KEY);
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_DETAIL_MARKDOWN_KEY + "*");
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_DETAIL_HTML_KEY + "*");
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_HOME_PAGE_LIST_KEY + "*");
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_CATEGORY_PAGE_LIST_KEY + "*");
        redisUtils.deleteKeysByPattern(RedisKey.ARTICLE_CONDITION_PAGE_LIST_KEY + "*");
    }
}

