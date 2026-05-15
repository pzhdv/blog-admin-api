package cn.pzhdv.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 文章-分类关联表
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:24:08
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("article_category_relation")
@ApiModel(value = "ArticleCategoryRelation对象", description = "文章-分类关联表")
public class ArticleCategoryRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @ApiModelProperty("ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章ID
     */
    @ApiModelProperty("文章ID")
    @TableField("article_id")
    private Long articleId;

    /**
     * 分类ID
     */
    @ApiModelProperty("分类ID")
    @TableField("category_id")
    private Long categoryId;
}
