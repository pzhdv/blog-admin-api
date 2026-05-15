package cn.pzhdv.blog.dto.article;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 文章标签修改DTO
 *
 * @author PanZonghui
 * @since 2026-05-11
 */
@Data
@ApiModel(value = "ArticleTagEditDTO", description = "文章标签修改请求参数")
public class ArticleTagEditDTO {

    @NotNull(message = "标签ID不能为空")
    @Positive(message = "标签ID必须为正整数")
    @ApiModelProperty(value = "标签ID（必填，必须为正整数）", required = true, example = "1001")
    private Long articleTagId;

    @NotBlank(message = "标签名称不能为空")
    @Length(min = 1, max = 30, message = "标签名称长度需在1-30个字符之间")
    @ApiModelProperty(value = "标签名称（必填，1-30个字符，不可重复）", required = true, example = "Spring Boot")
    private String articleTagName;

}
