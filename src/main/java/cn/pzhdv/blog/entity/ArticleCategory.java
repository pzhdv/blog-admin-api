package cn.pzhdv.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 文章分类表
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:07:36
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("article_category")
@ApiModel(value = "ArticleCategory对象", description = "文章分类表")
public class ArticleCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "分类ID", example = "1001")
    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;

    @ApiModelProperty(value = "分类名称", example = "后端开发")
    @TableField("category_name")
    private String categoryName;

    @ApiModelProperty(value = "父分类ID（根分类传0，子分类传父ID）", example = "0")
    @TableField("parent_id")
    private Integer parentId;

    @ApiModelProperty(value = "字体图标", example = "icon-book")
    @TableField("icon_class")
    private String iconClass;

    // ------------------------------ 非数据库字段 ------------------------------
    @ApiModelProperty(value = "分类下的文章总数", example = "100")
    @TableField(exist = false)
    private Long articleTotal;

    @ApiModelProperty(value = "子分类列表")
    @TableField(exist = false)
    private List<ArticleCategory> children;
}
