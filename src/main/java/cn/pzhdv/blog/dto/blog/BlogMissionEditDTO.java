package cn.pzhdv.blog.dto.blog;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 博客使命修改DTO
 *
 * @author PanZonghui
 * @since 2026-05-11
 */
@Data
@ApiModel(value = "BlogMissionEditDTO", description = "博客使命修改请求参数")
public class BlogMissionEditDTO {

    @NotNull(message = "使命ID不能为空")
    @Positive(message = "使命ID必须为正整数")
    @ApiModelProperty(value = "博客使命ID（必填，必须为正整数）", required = true, example = "1001")
    private Long missionId;

    @NotBlank(message = "使命标题不能为空")
    @Length(min = 1, max = 100, message = "使命标题长度需在1-100个字符之间")
    @ApiModelProperty(value = "使命标题（必填，1-100个字符，简洁概括使命核心）", required = true, example = "分享技术，助力成长")
    private String missionTitle;

    @NotBlank(message = "使命描述不能为空")
    @Length(min = 1, max = 500, message = "使命描述长度需在1-500个字符之间")
    @ApiModelProperty(value = "使命描述（必填，1-500个字符，详细说明使命意义）", required = true, example = "通过技术文章分享，帮助开发者解决实际问题，共同成长")
    private String missionDescription;

    @NotBlank(message = "使命要点不能为空")
    @Length(min = 1, max = 1000, message = "使命要点长度需在1-1000个字符之间")
    @ApiModelProperty(value = "具体使命要点（必填，1-1000个字符，用逗号/换行分隔要点）", required = true, example = "1. 输出高质量技术文章；2. 解答读者问题；3. 分享实战经验")
    private String missionPointListStr;

}
