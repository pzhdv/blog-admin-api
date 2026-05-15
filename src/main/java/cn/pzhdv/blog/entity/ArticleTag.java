package cn.pzhdv.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 文章标签表
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:07:36
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("article_tag")
@ApiModel(value = "ArticleTag对象", description = "文章标签表（标签名称唯一，支持新增/修改/删除）")
public class ArticleTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "标签ID", example = "1001")
    @TableId(value = "article_tag_id", type = IdType.AUTO)
    private Long articleTagId;

    @ApiModelProperty(value = "标签名称", example = "Spring Boot")
    @TableField("article_tag_name")
    private String articleTagName;
}