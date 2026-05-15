package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.constant.RedisKey;
import cn.pzhdv.blog.dto.blog.BlogMissionAddDTO;
import cn.pzhdv.blog.dto.blog.BlogMissionEditDTO;
import cn.pzhdv.blog.entity.BlogMission;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.BlogMissionService;
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
 * 博客使命表 前端控制器
 * 管理博客使命信息（仅支持单条数据，适配前端关于页/首页使命展示场景）
 *
 * @author PanZonghui
 * @since 2025-06-25 21:03:51
 */
@Slf4j
@Validated
@Api(tags = "博客使命管理")
@RestController
@RequestMapping("/blogMission")
@RequiredArgsConstructor
public class BlogMissionController {

    private final RedisUtils redisUtils;
    private final BlogMissionService baseService;

    @ApiLog("查询博客使命信息")
    @ApiOperation(
            value = "查询博客使命信息",
            notes = "获取系统中唯一的博客使命详情，用于前端关于页/首页展示",
            httpMethod = "GET",
            produces = "application/json")
    @GetMapping("blogMissionInfo")
    public Result<BlogMission> getBlogMissionInfo() {
        String cacheKey = RedisKey.BLOG_MISSION_CACHE_KEY;
        BlogMission blogMission = redisUtils.get(cacheKey, BlogMission.class);

        // 缓存未命中，查询数据库
        if (blogMission == null) {
            log.debug("博客使命缓存未命中，从数据库查询 | key: {}", cacheKey);
            blogMission = baseService.getUniqueMission();

            // 存入缓存（仅当数据存在时，设置合理过期时间）
            if (blogMission != null) {
                int expireSeconds = CacheExpireUtil.getDefaultExpireSeconds(); // 复用缓存过期工具类
                redisUtils.set(cacheKey, blogMission, expireSeconds);
                log.debug("博客使命写入缓存成功 | key: {}, 过期时间: {}秒", cacheKey, expireSeconds);
            } else {
                log.warn("数据库中无博客使命信息 | key: {}", cacheKey);
            }
        } else {
            log.debug("博客使命命中缓存 | key: {}", cacheKey);
        }

        return ResultUtil.ok(blogMission);
    }

    @ApiLog("新增博客使命")
    @ApiOperation(
            value = "新增博客使命",
            notes = "添加系统唯一的博客使命信息，仅允许添加一条",
            httpMethod = "POST",
            produces = "application/json")
    @PostMapping("add")
    public Result<Boolean> addBlogMission(@RequestBody @Validated BlogMissionAddDTO addDTO) {

        // 业务校验：仅允许添加一条使命信息
        long missionCount = baseService.count();
        if (missionCount > 0) {
            log.warn("博客使命添加失败 | 已存在使命信息，不允许重复添加");
            return ResultUtil.error(ResultCode.RESOURCE_OCCUPIED, "博客使命仅允许添加一条，如需修改请使用编辑功能");
        }

        // DTO转实体
        BlogMission blogMission = new BlogMission();
        BeanUtils.copyProperties(addDTO, blogMission);

        // 执行添加操作
        boolean success = baseService.save(blogMission);
        if (!success) {
            log.error("博客使命添加失败 | 数据库保存失败 | 使命标题: {}", blogMission.getMissionTitle());
            return ResultUtil.error(ResultCode.ADD_FAIL, "博客使命添加失败，请重试");
        }

        // 清除缓存（确保新增后查询能获取最新数据）
        redisUtils.del(RedisKey.BLOG_MISSION_CACHE_KEY);
        log.info("博客使命添加成功 | 使命ID: {}, 使命标题: {}", blogMission.getMissionId(), blogMission.getMissionTitle());
        return ResultUtil.ok(true);
    }

    @ApiLog("编辑博客使命")
    @ApiOperation(
            value = "编辑博客使命",
            notes = "更新博客使命信息，需传递完整信息（含missionId）",
            httpMethod = "PUT",
            produces = "application/json")
    @PutMapping("update")
    public Result<Boolean> updateBlogMission(@RequestBody @Validated BlogMissionEditDTO editDTO) {

        // DTO转实体
        BlogMission blogMission = new BlogMission();
        BeanUtils.copyProperties(editDTO, blogMission);

        // 执行修改操作
        boolean success = baseService.updateById(blogMission);
        if (!success) {
            log.error("博客使命修改失败 | 数据库更新失败 | 使命ID: {}", blogMission.getMissionId());
            return ResultUtil.error(ResultCode.UPDATE_FAIL, "博客使命修改失败，请重试");
        }

        // 清除缓存（确保修改后前端能获取最新数据）
        redisUtils.del(RedisKey.BLOG_MISSION_CACHE_KEY);
        log.info("博客使命修改成功 | 使命ID: {}", blogMission.getMissionId());
        return ResultUtil.ok(true);
    }

    @ApiLog("删除博客使命")
    @ApiOperation(
            value = "删除博客使命",
            notes = "谨慎操作！删除后需重新添加，适用于重置博客使命场景",
            httpMethod = "DELETE",
            produces = "application/json")
    @DeleteMapping("delete/{missionId}")
    public Result<Boolean> deleteBlogMission(
            @ApiParam(name = "missionId", value = "博客使命ID（≥1）", required = true)
            @PathVariable("missionId")
            @Min(value = 1, message = "使命ID必须为正整数")
            Long missionId) {

        // 前置校验：使命是否存在
        if (baseService.getById(missionId) == null) {
            log.warn("博客使命删除失败 | 使命不存在 | 使命ID: {}", missionId);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "博客使命不存在，无法删除");
        }

        // 执行删除操作
        boolean success = baseService.removeById(missionId);
        if (!success) {
            log.error("博客使命删除失败 | 数据库删除失败 | 使命ID: {}", missionId);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "博客使命删除失败，请重试");
        }

        // 清除缓存（确保删除后无缓存残留）
        redisUtils.del(RedisKey.BLOG_MISSION_CACHE_KEY);
        log.info("博客使命删除成功 | 使命ID: {}", missionId);
        return ResultUtil.ok(true);
    }
}