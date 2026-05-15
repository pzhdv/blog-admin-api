package cn.pzhdv.blog.dto.author;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/**
 * 工作经历修改DTO
 *
 * @author PanZonghui
 * @since 2026-05-11
 */
@Data
@ApiModel(value = "JobExperienceEditDTO", description = "工作经历修改请求参数")
public class JobExperienceEditDTO {

    @NotNull(message = "经历ID不能为空")
    @Positive(message = "经历ID必须为正整数")
    @ApiModelProperty(value = "经历ID", required = true, example = "1001")
    private Long id;

    @NotBlank(message = "经历标题不能为空")
    @Length(min = 1, max = 50, message = "经历标题长度必须在1-50个字符之间")
    @ApiModelProperty(value = "经历标题", required = true, example = "Java开发工程师")
    private String title;

    @NotBlank(message = "所属组织不能为空")
    @Length(min = 1, max = 50, message = "所属组织长度必须在1-50个字符之间")
    @ApiModelProperty(value = "所属组织/公司", required = true, example = "XX科技有限公司")
    private String organization;

    @NotBlank(message = "时间范围不能为空")
    @Length(min = 1, max = 20, message = "时间范围长度必须在1-20个字符之间")
    @ApiModelProperty(value = "工作时间范围", required = true, example = "2022.01-2024.12")
    private String timeRange;

    @NotBlank(message = "工作成就不能为空")
    @Length(min = 1, max = 1000, message = "工作成就长度必须在1-1000个字符之间")
    @ApiModelProperty(value = "工作成就描述", required = true, example = "1.负责用户中心模块开发；2.优化接口响应速度30%")
    private String achievementListStr;

    @Length(max = 50, message = "标题图标长度不能超过50个字符")
    @ApiModelProperty(value = "标题图标样式类", example = "icon-briefcase")
    private String titleIcon;

}