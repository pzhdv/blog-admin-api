package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.constant.RedisKey;
import cn.pzhdv.blog.dto.author.JobExperienceAddDTO;
import cn.pzhdv.blog.dto.author.JobExperienceEditDTO;
import cn.pzhdv.blog.entity.JobExperience;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.JobExperienceService;
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

/**
 * 工作经历表 前端控制器
 * 管理工作经历信息（支持增删改查，用于前端个人简介页面展示）
 *
 * @author PanZonghui
 * @since 2025-06-25 21:03:51
 */
@Slf4j
@Validated
@Api(tags = "工作经历管理")
@RestController
@RequestMapping("/jobExperience")
@RequiredArgsConstructor
public class JobExperienceController {

    private final JobExperienceService jobExperienceService;
    private final RedisUtils redisUtils;

    @ApiLog("查询工作经历列表")
    @ApiOperation(
            value = "查询工作经历列表",
            notes = "获取所有工作经历信息（按时间倒序），用于前端个人简介页面展示",
            httpMethod = "GET",
            produces = "application/json")
    @GetMapping("list")
    public Result<List<JobExperience>> listAllJobExperiences() {
        String cacheKey = RedisKey.JOB_EXPERIENCE_CACHE_KEY;

        List<JobExperience> experienceList = redisUtils.get(cacheKey, new TypeReference<>() {
        });

        // 缓存未命中，查询数据库
        if (experienceList == null) {
            log.debug("工作经历缓存未命中，从数据库查询 | key: {}", cacheKey);
            experienceList = jobExperienceService.queryJobExperienceList();

            // 存入缓存（仅当数据存在时，设置合理过期时间）
            if (experienceList != null && !experienceList.isEmpty()) {
                int expireSeconds = CacheExpireUtil.getDefaultExpireSeconds();
                redisUtils.set(cacheKey, experienceList, expireSeconds);
                log.debug("工作经历写入缓存成功 | key: {}, 过期时间: {}秒, 数据条数: {}",
                        cacheKey, expireSeconds, experienceList.size());
            } else {
                log.warn("数据库中无工作经历数据 | key: {}", cacheKey);
            }
        } else {
            log.debug("工作经历命中缓存 | key: {}, 数据条数: {}", cacheKey, experienceList.size());
        }

        return ResultUtil.ok(experienceList);
    }

    @ApiLog("新增工作经历")
    @ApiOperation(
            value = "新增工作经历",
            notes = "新增一条工作经历信息（无需传ID，标题/组织/时间/成就为必填）",
            httpMethod = "POST",
            produces = "application/json")
    @PostMapping("add")
    public Result<Boolean> add(@RequestBody @Validated JobExperienceAddDTO addDTO) {
        // DTO转实体
        JobExperience experience = new JobExperience();
        BeanUtils.copyProperties(addDTO, experience);

        // 执行添加操作
        boolean flag = jobExperienceService.save(experience);
        if (!flag) {
            log.error("工作经历添加失败 | 数据库保存失败 | 经历标题: {}", experience.getTitle());
            return ResultUtil.error(ResultCode.ADD_FAIL, "工作经历添加失败，请重试");
        }

        // 清除缓存
        redisUtils.del(RedisKey.JOB_EXPERIENCE_CACHE_KEY);
        log.info("工作经历添加成功 | 经历ID: {}, 标题: {}", experience.getId(), experience.getTitle());
        return ResultUtil.ok(true);
    }

    @ApiLog("编辑工作经历")
    @ApiOperation(
            value = "编辑工作经历",
            notes = "更新一条工作经历信息（必须传ID，标题/组织/时间/成就为必填）",
            httpMethod = "PUT",
            produces = "application/json")
    @PutMapping("update")
    public Result<Boolean> update(@RequestBody @Validated JobExperienceEditDTO editDTO) {
        // DTO转实体
        JobExperience experience = new JobExperience();
        BeanUtils.copyProperties(editDTO, experience);

        // 执行修改操作
        boolean flag = jobExperienceService.updateById(experience);
        if (!flag) {
            log.error("工作经历修改失败 | 数据库更新失败 | 经历ID: {}", experience.getId());
            return ResultUtil.error(ResultCode.UPDATE_FAIL, "工作经历修改失败，请重试");
        }

        // 清除缓存
        redisUtils.del(RedisKey.JOB_EXPERIENCE_CACHE_KEY);
        log.info("工作经历修改成功 | 经历ID: {}, 标题: {}", experience.getId(), experience.getTitle());
        return ResultUtil.ok(true);
    }

    @ApiLog("删除工作经历")
    @ApiOperation(
            value = "删除工作经历",
            notes = "根据ID删除一条工作经历信息",
            httpMethod = "DELETE",
            produces = "application/json")
    @DeleteMapping("delete/{id}")
    public Result<Boolean> deleteById(
            @PathVariable @ApiParam(name = "id", value = "工作经历ID（≥1）", required = true)
            @Min(value = 1, message = "经历ID必须为正整数")
            Long id) {

        // 执行删除操作
        boolean flag = jobExperienceService.removeById(id);
        if (!flag) {
            log.error("工作经历删除失败 | 数据库删除失败 | 经历ID: {}", id);
            return ResultUtil.error(ResultCode.DELETE_FAIL, "工作经历删除失败，请重试");
        }

        // 清除缓存
        redisUtils.del(RedisKey.JOB_EXPERIENCE_CACHE_KEY);
        log.info("工作经历删除成功 | 经历ID: {}", id);
        return ResultUtil.ok(true);
    }

}