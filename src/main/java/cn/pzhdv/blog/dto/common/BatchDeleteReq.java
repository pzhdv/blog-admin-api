package cn.pzhdv.blog.dto.common;

import cn.pzhdv.blog.exception.BusinessException;
import cn.pzhdv.blog.result.ResultCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Objects;

/**
 * 通用批量删除请求DTO（专用于Long类型ID，ID≥1）
 * 全局复用：用户、角色、菜单、部门等所有Long类型主键的批量删除场景
 */
@ApiModel(value = "BatchDeleteReq", description = "通用批量删除请求（Long类型ID，ID≥1）")
public final class BatchDeleteReq {

    @ApiModelProperty(
            value = "待删除的主键ID列表",
            required = true,
            example = "[1,2,3]",
            notes = "ID必须为正整数（≥1），列表不能为空且不可包含null")
    @NotEmpty(message = "请选择待删除的记录（ID列表不能为空）")
    private List<@Positive(message = "ID必须为正整数（≥1）") Long> ids;

    // 私有化构造器，强制通过工厂方法创建
    private BatchDeleteReq() {}

    /**
     * 工厂方法创建实例
     * @param ids 待删除ID列表（非空、无null、元素≥1）
     * @return BatchDeleteReq实例
     * @throws BusinessException 若ids为null/包含null元素
     */
    public static BatchDeleteReq of(List<Long> ids) {
        // 替换为业务异常，关联对应ResultCode
        if (Objects.isNull(ids)) {
            throw new BusinessException(ResultCode.PARAM_REQUIRED, "ID列表");
        }
        if (ids.contains(null)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "ID列表中不能包含null元素");
        }
        BatchDeleteReq req = new BatchDeleteReq();
        req.setIds(ids);
        return req;
    }

    public List<Long> getIds() {
        // 返回不可变列表，避免外部修改内部数据
        return List.copyOf(ids);
    }

    public void setIds(List<Long> ids) {
        // 替换为业务异常，关联对应ResultCode
        if (Objects.isNull(ids)) {
            throw new BusinessException(ResultCode.PARAM_REQUIRED, "ID列表");
        }
        if (ids.contains(null)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "ID列表中不能包含null元素");
        }
        // 存储不可变列表，增强数据安全性
        this.ids = List.copyOf(ids);
    }
}