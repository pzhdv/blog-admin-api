package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.dto.common.BatchDeleteReq;
import cn.pzhdv.blog.entity.SysOperationLog;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.service.SysOperationLogService;
import cn.pzhdv.blog.utils.QueryWrapperUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/sys-operation-log")
@RequiredArgsConstructor
@Api(tags = "系统操作日志管理")
@Validated
public class SysOperationLogController {

    private final SysOperationLogService sysOperationLogService;

    @ApiOperation(value = "操作日志条件分页查询", notes = "支持用户名、请求路径、请求方式、耗时范围、时间范围查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "query", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "requestUrl", value = "请求路径", paramType = "query", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "requestMethod", value = "请求方式", paramType = "query", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "minCostTime", value = "最小耗时", paramType = "query", dataType = "Long", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "maxCostTime", value = "最大耗时", paramType = "query", dataType = "Long", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "beginTime", value = "开始日期 yyyy-MM-dd HH:mm:ss", paramType = "query", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endTime", value = "结束日期 yyyy-MM-dd HH:mm:ss", paramType = "query", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "页码", paramType = "query", dataType = "Integer", dataTypeClass = Integer.class, defaultValue = "1"),
            @ApiImplicitParam(name = "size", value = "每页条数", paramType = "query", dataType = "Integer", dataTypeClass = Integer.class, defaultValue = "10")
    })
    @GetMapping("/page")
    public Result<Page<SysOperationLog>> page(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String requestUrl,
            @RequestParam(required = false) String requestMethod,
            @RequestParam(required = false) Long minCostTime,
            @RequestParam(required = false) Long maxCostTime,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") @Min(1) Integer current,
            @RequestParam(defaultValue = "10") @Min(1) Integer size) {

        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaEqCondition(wrapper, SysOperationLog::getUsername, username);
        QueryWrapperUtil.addLambdaLikeCondition(wrapper, SysOperationLog::getRequestUrl, requestUrl);
        QueryWrapperUtil.addLambdaEqCondition(wrapper, SysOperationLog::getRequestMethod, requestMethod);
        QueryWrapperUtil.addLambdaGeCondition(wrapper, SysOperationLog::getCostTime, minCostTime);
        QueryWrapperUtil.addLambdaLeCondition(wrapper, SysOperationLog::getCostTime, maxCostTime);
        QueryWrapperUtil.addLambdaGeCondition(wrapper, SysOperationLog::getCreateTime, beginTime);
        QueryWrapperUtil.addLambdaLeCondition(wrapper, SysOperationLog::getCreateTime, endTime);

        // 稳定排序
        wrapper.orderByDesc(SysOperationLog::getCreateTime)
                .orderByDesc(SysOperationLog::getId);

        Page<SysOperationLog> page = new Page<>(current, size);
        sysOperationLogService.page(page, wrapper);

        log.info("分页查询操作日志成功");
        return ResultUtil.ok(page);
    }

    @ApiOperation("根据ID删除日志")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable @Min(1) Long id) {
        SysOperationLog logEntity = sysOperationLogService.getById(id);
        if (Objects.isNull(logEntity)) {
            log.warn("删除失败，日志不存在：{}", id);
            return ResultUtil.error(ResultCode.DATA_NOT_FOUND, "日志不存在");
        }

        boolean success = sysOperationLogService.removeById(id);
        if (!success) {
            return ResultUtil.error(ResultCode.DELETE_FAIL, "删除失败");
        }

        log.info("删除日志成功：{}", id);
        return ResultUtil.ok(true);
    }

    @ApiOperation("批量删除系统日志")
    @DeleteMapping("/delete/batch")
    public Result<Boolean> deleteBatch(@RequestBody @Valid BatchDeleteReq deleteReq) {
        List<Long> ids = deleteReq.getIds();
        if (ids.isEmpty()) {
            return ResultUtil.error(ResultCode.DELETE_FAIL, "ID 不能为空");
        }

        // 校验存在
        List<SysOperationLog> existList = sysOperationLogService.listByIds(ids);
        if (existList.size() != ids.size()) {
            return ResultUtil.error(ResultCode.DELETE_FAIL, "部分日志不存在或已删除");
        }

        boolean success = sysOperationLogService.removeByIds(ids);
        if (!success) {
            return ResultUtil.error(ResultCode.DELETE_FAIL, "批量删除失败");
        }

        log.info("批量删除日志成功，数量：{}", ids.size());
        return ResultUtil.ok(true);
    }
}