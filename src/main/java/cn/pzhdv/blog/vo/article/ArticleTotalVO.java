package cn.pzhdv.blog.vo.article;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "文章总数实体类", description = "文章总数实体类(总数，已发布，草稿)")
public class ArticleTotalVO {

    @ApiModelProperty(value = "文章总数", example = "100")
    private Long totalCount;

    @ApiModelProperty(value = "已发布数量", example = "95")
    private Long publishedCount;

    @ApiModelProperty(value = "草稿数量", example = "5")
    private Long draftCount;
}
