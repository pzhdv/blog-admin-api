package cn.pzhdv.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * <p>
 * 博客文章表
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:24:08
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("article")
@ApiModel(value = "Article对象", description = "博客文章表（含基础信息、分类/标签关联ID）")
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    // ------------------------------ 数据库主键（含校验） ------------------------------
    @ApiModelProperty(value = "文章ID（修改必须传正整数）", example = "1001")
    @TableId(value = "article_id", type = IdType.AUTO)
    private Long articleId;

    // ------------------------------ 核心业务字段（新增/修改都需校验） ------------------------------
    @ApiModelProperty(
            value = "文章标题（必填）",
            required = true,
            example = "Spring Boot 缓存优化实践")
    @TableField("title")
    @NotBlank(message = "文章标题不能为空")
    private String title;

    @ApiModelProperty(
            value = "文章Markdown内容（必填）",
            required = true,
            example = "# 标题\n\n内容详情...")
    @TableField("markdown")
    @NotBlank(message = "文章内容不能为空")
    private String markdown;

    @ApiModelProperty(value = "封面图路径/URL（选填，最大255个字符）", example = "https://xxx.com/cover.jpg")
    @TableField("image")
    @Length(max = 255, message = "封面图URL长度不能超过255个字符")
    private String image;

    @ApiModelProperty(value = "文章摘要（选填）", example = "本文介绍Spring Boot缓存优化的3种方案...")
    @TableField("excerpt")
    private String excerpt;

    @ApiModelProperty(value = "发布状态（必填，true=已发布，false=草稿）", required = true, example = "true")
    @TableField("publish_state")
    @NotNull(message = "发布状态不能为空（true=已发布，false=草稿）")
    private Boolean publishState;

    @ApiModelProperty(value = "推荐权重（选填，0-100，数值越大优先级越高）", example = "50")
    @TableField("recommend_weight")
    @PositiveOrZero(message = "推荐权重不能为负数")
    @Max(value = 100, message = "推荐权重最大为100")
    private Integer recommendWeight;

    // ------------------------------ 自动填充字段（无需手动传值） ------------------------------
    @ApiModelProperty(value = "创建时间（自动填充，无需传值）", example = "2025-06-25 10:30:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @ApiModelProperty(value = "修改时间（自动填充，无需传值）", example = "2025-06-25 11:45:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    // ------------------------------ 非数据库字段（关联查询/参数传递用） ------------------------------
    @ApiModelProperty(
            value = "关联分类ID列表（可传可不传，传则至少1个，用于绑定分类）",
            required = false, // 明确标记为非必选
            example = "[1,2]")
    @TableField(exist = false)
    // 核心：仅校验“传值时的规则”——非null且非空列表（min=1），不传则跳过
    @Size(
            min = 1,
            message = "选择分类时至少1个")
    private List<Long> categoryIds;

    @ApiModelProperty(
            value = "关联标签ID列表（可传可不传，传则至少1个，用于绑定标签）",
            required = false, // 明确标记为非必选
            example = "[3,4]")
    @TableField(exist = false)
    // 核心：同样仅校验“传值时的规则”，与分类保持逻辑统一
    @Size(
            min = 1,
            message = "选择标签时至少1个")
    private List<Long> tagIds;

    @ApiModelProperty(value = "关联分类列表（查询详情时返回，无需传值）")
    @TableField(exist = false)
    private List<ArticleCategory> articleCategoryList;

    @ApiModelProperty(value = "关联标签列表（查询详情时返回，无需传值）")
    @TableField(exist = false)
    private List<ArticleTag> articleTagList;
}
