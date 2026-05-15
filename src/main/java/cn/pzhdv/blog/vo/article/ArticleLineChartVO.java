package cn.pzhdv.blog.vo.article;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "文章折线图数据实体", description = "文章折线图数据(月对应的数据)")
public class ArticleLineChartVO {

    @ApiModelProperty(value = "年月", example = "2025-01")
    private String ym;

    @ApiModelProperty(value = "当月已发布的文章总数", example = "10")
    private Long publishedCnt;

    @ApiModelProperty(value = "当月发布的草稿总数", example = "0")
    private Long draftCnt;
}