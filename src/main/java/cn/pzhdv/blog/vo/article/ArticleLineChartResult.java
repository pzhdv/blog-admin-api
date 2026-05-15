package cn.pzhdv.blog.vo.article;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "文章折线图数据", description = "文章折线图数据(月对应的数据)")
public class ArticleLineChartResult {

    @ApiModelProperty(value = "年月列表")
    private List<String> ymList;

    @ApiModelProperty(value = "已发布列表")
    private List<Long> publishedCntList;

    @ApiModelProperty(value = "草稿列表")
    private List<Long> draftCntList;
}
