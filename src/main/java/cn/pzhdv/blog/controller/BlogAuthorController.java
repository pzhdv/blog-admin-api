package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.constant.RedisKey;
import cn.pzhdv.blog.dto.author.BlogAuthorAddDTO;
import cn.pzhdv.blog.dto.author.BlogAuthorEditDTO;
import cn.pzhdv.blog.entity.BlogAuthor;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.BlogAuthorService;
import cn.pzhdv.blog.utils.CacheExpireUtil;
import cn.pzhdv.blog.utils.RedisUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;

/**
 * 用户个人信息表 前端控制器 管理博客作者信息（仅支持单条数据，适配前端个人中心编辑场景）
 *
 * @author PanZonghui
 * @since 2025-06-25 21:03:51
 */
@Slf4j
@Validated
@Api(tags = "博客作者信息管理")
@RestController
@RequestMapping("/blogAuthor")
@RequiredArgsConstructor
public class BlogAuthorController {

    private final RedisUtils redisUtils;
    private final BlogAuthorService service;

    @ApiLog("获取当前作者信息")
    @ApiOperation(
            value = "获取当前作者信息",
            notes = "用于前端个人中心展示，返回系统中唯一的作者信息（无数据则返回null）",
            httpMethod = "GET",
            produces = "application/json")
    @GetMapping("currentUserInfo")
    public Result<BlogAuthor> getCurrentAuthorInfo() {
        String cacheKey = RedisKey.BLOG_AUTHOR_CACHE_KEY;
        // 优化：指定泛型类型，避免反序列化异常
        BlogAuthor author = redisUtils.get(cacheKey, BlogAuthor.class);

        // 缓存未命中，查询数据库（作者信息仅一条）
        if (author == null) {
            log.debug("作者信息缓存未命中，从数据库查询 | key: {}", cacheKey);
            author = service.getUniqueAuthor();

            // 存入缓存（仅当数据存在时）
            if (author != null) {
                int expireSeconds = CacheExpireUtil.getDefaultExpireSeconds();
                redisUtils.set(cacheKey, author, expireSeconds);
                log.debug("作者信息写入缓存成功 | key: {}, 过期时间: {}秒", cacheKey, expireSeconds);
            } else {
                log.warn("数据库中无作者信息 | key: {}", cacheKey);
            }
        } else {
            log.debug("作者信息命中缓存 | key: {}", cacheKey);
        }

        return ResultUtil.ok(author);
    }

    @ApiLog("新增作者信息")
    @ApiOperation(
            value = "新增作者信息",
            notes = "仅允许添加一条作者信息（博客系统唯一），用于初始化个人资料",
            httpMethod = "POST",
            produces = "application/json")
    @PostMapping("add")
    public Result<Boolean> addAuthor(@RequestBody @Validated BlogAuthorAddDTO addDTO) {

        // 校验：是否已存在作者信息（仅允许一条）
        long authorCount = service.count();
        if (authorCount > 0) {
            log.warn("作者信息添加失败 | 已存在作者信息，不允许重复添加");
            return ResultUtil.error(ResultCode.RESOURCE_OCCUPIED, "系统已存在作者信息，如需修改请使用编辑功能");
        }

        // DTO转实体
        BlogAuthor blogAuthor = new BlogAuthor();
        BeanUtils.copyProperties(addDTO, blogAuthor);

        // 执行添加操作
        boolean success = service.save(blogAuthor);
        if (!success) {
            log.error("作者信息添加失败 | 数据库保存失败 | 作者姓名: {}", blogAuthor.getFullName());
            return ResultUtil.error(ResultCode.ADD_FAIL);
        }

        // 清除缓存（确保新增后查询能获取最新数据）
        redisUtils.del(RedisKey.BLOG_AUTHOR_CACHE_KEY);
        log.info("作者信息添加成功 | 作者ID: {}, 姓名: {}", blogAuthor.getUserId(), blogAuthor.getFullName());
        return ResultUtil.ok(true);
    }

    @ApiLog("编辑作者信息")
    @ApiOperation(
            value = "编辑作者信息",
            notes = "用于编辑个人资料，需传递完整作者信息（含userId）",
            httpMethod = "PUT",
            produces = "application/json")
    @PutMapping("update")
    public Result<Boolean> updateAuthor(@RequestBody @Validated BlogAuthorEditDTO editDTO) {

        // DTO转实体
        BlogAuthor blogAuthor = new BlogAuthor();
        BeanUtils.copyProperties(editDTO, blogAuthor);

        // 执行修改操作
        boolean success = service.updateById(blogAuthor);
        if (!success) {
            log.error("作者信息修改失败 | 数据库更新失败 | 作者ID: {}", blogAuthor.getUserId());
            return ResultUtil.error(ResultCode.UPDATE_FAIL);
        }

        // 清除缓存（确保修改后查询能获取最新数据）
        redisUtils.del(RedisKey.BLOG_AUTHOR_CACHE_KEY);
        log.info("作者信息修改成功 | 作者ID: {}", blogAuthor.getUserId());
        return ResultUtil.ok(true);
    }

    @ApiLog("删除作者信息")
    @ApiOperation(
            value = "删除作者信息",
            notes = "谨慎操作！删除后需重新添加，适用于重置个人资料场景",
            httpMethod = "DELETE",
            produces = "application/json")
    @DeleteMapping("delete/{userId}")
    public Result<Boolean> deleteAuthor(
            @PathVariable @ApiParam(name = "userId", value = "作者ID（必须为正整数）", required = true)
            @Min(value = 1, message = "作者ID必须为正整数")
            Long userId) {

        // 执行删除操作
        boolean success = service.removeById(userId);
        if (!success) {
            log.error("作者信息删除失败 | 数据库删除失败 | 作者ID: {}", userId);
            return ResultUtil.error(ResultCode.DELETE_FAIL);
        }

        // 清除缓存（确保删除后查询无缓存残留）
        redisUtils.del(RedisKey.BLOG_AUTHOR_CACHE_KEY);
        log.info("作者信息删除成功 | 作者ID: {}", userId);
        return ResultUtil.ok(true);
    }
}
