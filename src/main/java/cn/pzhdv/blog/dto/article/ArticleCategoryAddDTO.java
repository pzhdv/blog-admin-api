package cn.pzhdv.blog.dto.article;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

/**
 * 文章分类新增DTO
 *
 * @author PanZonghui
 * @since 2026-05-11
 */
@Data
@ApiModel(value = "ArticleCategoryAddDTO", description = "文章分类新增请求参数")
public class ArticleCategoryAddDTO {

    @NotBlank(message = "分类名称不能为空")
    @Length(min = 1, max = 50, message = "分类名称长度需在1-50个字符之间")
    @ApiModelProperty(value = "分类名称（必填，1-50个字符）", required = true, example = "后端开发")
    private String categoryName;

    @NotNull(message = "父分类ID不能为空")
    @PositiveOrZero(message = "父分类ID必须为非负整数（根分类传0，子分类传父ID）")
    @ApiModelProperty(value = "父分类ID（必填，根分类传0，子分类传父ID）", required = true, example = "0")
    private Integer parentId;

    @Length(max = 100, message = "字体图标长度不能超过100个字符")
    @ApiModelProperty(value = "字体图标（选填，传则最大100个字符）", example = "icon-book")
    private String iconClass;

}
