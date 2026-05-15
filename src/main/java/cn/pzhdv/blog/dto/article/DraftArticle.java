package cn.pzhdv.blog.dto.article;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ApiModel(value = "草稿Article对象", description = "保存草稿文章")
public class DraftArticle {

    @ApiModelProperty(value = "文章ID（新增无需传，修改必须传正整数）", example = "1001")
    private Long articleId;

    @ApiModelProperty(value = "文章标题（必填，1-200个字符，不可重复）", required = true, example = "Spring Boot 缓存优化实践")
    @NotBlank(message = "文章标题不能为空")
    @Length(min = 1, max = 200, message = "文章标题长度需在1-200个字符之间")
    private String title;

    @ApiModelProperty(value = "文章Markdown内容（必填，至少10个字符）", required = true, example = "# 标题\n\n内容详情...")
    @TableField("markdown")
    @NotBlank(message = "文章内容不能为空")
    private String markdown;
}
