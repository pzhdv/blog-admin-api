package cn.pzhdv.blog.dto.article;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;
import java.util.List;

/**
 * 文章修改请求DTO
 * <p>
 * 接收前端修改文章的参数，包含文章ID、标题、正文、发布状态等核心字段，
 * 所有字段均添加校验规则，确保入参符合业务规范。
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11
 */
@Data
@ApiModel(value = "ArticleEditDTO", description = "文章修改请求参数")
public class ArticleEditDTO {

    /**
     * 文章ID
     * <p>必填，用于定位要修改的文章，必须为正整数</p>
     */
    @NotNull(message = "文章ID不能为空")
    @Positive(message = "文章ID必须为正整数")
    @ApiModelProperty(value = "文章ID（必填，必须为正整数）", required = true, example = "1001")
    private Long articleId;

    /**
     * 文章标题
     * <p>必填，用于标识文章主题</p>
     */
    @NotBlank(message = "文章标题不能为空")
    @ApiModelProperty(value = "文章标题（必填）", required = true, example = "Spring Boot 缓存优化实践")
    private String title;

    /**
     * 文章Markdown内容
     * <p>必填，文章正文内容</p>
     */
    @NotBlank(message = "文章内容不能为空")
    @ApiModelProperty(value = "文章Markdown内容（必填）", required = true, example = "# 标题\n\n内容详情...")
    private String markdown;

    /**
     * 封面图路径/URL
     * <p>选填，最大255个字符</p>
     */
    @Length(max = 255, message = "封面图URL长度不能超过255个字符")
    @ApiModelProperty(value = "封面图路径/URL（选填，最大255个字符）", example = "https://xxx.com/cover.jpg")
    private String image;

    /**
     * 文章摘要
     * <p>选填，文章的简短描述</p>
     */
    @ApiModelProperty(value = "文章摘要（选填）", example = "本文介绍Spring Boot缓存优化的3种方案...")
    private String excerpt;

    /**
     * 发布状态
     * <p>必填，true=已发布，false=草稿</p>
     */
    @NotNull(message = "发布状态不能为空（true=已发布，false=草稿）")
    @ApiModelProperty(value = "发布状态（必填，true=已发布，false=草稿）", required = true, example = "true")
    private Boolean publishState;

    /**
     * 推荐权重
     * <p>选填，0-100，数值越大优先级越高</p>
     */
    @PositiveOrZero(message = "推荐权重不能为负数")
    @Max(value = 100, message = "推荐权重最大为100")
    @ApiModelProperty(value = "推荐权重（选填，0-100，数值越大优先级越高）", example = "50")
    private Integer recommendWeight;

    /**
     * 关联分类ID列表
     * <p>选填，传则至少1个，用于绑定分类</p>
     */
    @Size(min = 1, message = "选择分类时至少1个")
    @ApiModelProperty(value = "关联分类ID列表（可传可不传，传则至少1个，用于绑定分类）", example = "[1,2]")
    private List<Long> categoryIds;

    /**
     * 关联标签ID列表
     * <p>选填，传则至少1个，用于绑定标签</p>
     */
    @Size(min = 1, message = "选择标签时至少1个")
    @ApiModelProperty(value = "关联标签ID列表（可传可不传，传则至少1个，用于绑定标签）", example = "[3,4]")
    private List<Long> tagIds;

}
