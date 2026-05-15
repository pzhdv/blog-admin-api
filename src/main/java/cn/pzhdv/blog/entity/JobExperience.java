package cn.pzhdv.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 工作经历表（存储个人工作/成就经历，用于前端个人简介页面展示）
 * </p>
 *
 * @author PanZonghui
 * @since 2025-06-25 21:03:51
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("job_experience")
@ApiModel(value = "JobExperience对象", description = "工作经历表（含职位、组织、时间范围、成就等信息）")
public class JobExperience implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "经历ID", example = "1001")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "经历标题", example = "Java开发工程师")
    @TableField("title")
    private String title;

    @ApiModelProperty(value = "所属组织", example = "XX科技有限公司")
    @TableField("organization")
    private String organization;

    @ApiModelProperty(value = "时间范围", example = "2022.01-2024.12")
    @TableField("time_range")
    private String timeRange;

    @ApiModelProperty(value = "成就列表", example = "1. 负责用户中心模块开发；2. 优化接口响应速度30%")
    @TableField("achievement_list_str")
    private String achievementListStr;

    @ApiModelProperty(value = "标题图标类", example = "icon-briefcase")
    @TableField("title_icon")
    private String titleIcon;
}
