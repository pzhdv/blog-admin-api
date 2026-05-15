package cn.pzhdv.blog.vo.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 文章分类统计饼图数据实体
 * 对应每个分类的文章数量，用于前端 ECharts 等统计图表展示
 */
@Data
@ApiModel(value = "CategoryPieChartVO", description = "文章分类统计饼图数据实体")
public class CategoryPieChartVO {

    /**
     * 分类名称
     */
    @ApiModelProperty(value = "分类名称", example = "Java")
    private String categoryName;

    /**
     * 本分类关联的文章数量
     */
    @ApiModelProperty(value = "本分类关联的文章数量", example = "12")
    private Long articleCount;
}
