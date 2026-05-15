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
 * 博客使命表（存储博客的核心使命信息，系统仅支持单条数据）
 * </p>
 *
 * @author PanZonghui
 * @since 2025-06-25 21:03:51
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("blog_mission")
@ApiModel(value = "BlogMission对象", description = "博客使命表（含使命标题、描述、要点，仅支持单条数据管理）")
public class BlogMission implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "博客使命ID", example = "1001")
    @TableId(value = "mission_id", type = IdType.AUTO)
    private Long missionId;

    @ApiModelProperty(value = "使命标题", example = "分享技术，助力成长")
    @TableField("mission_title")
    private String missionTitle;

    @ApiModelProperty(value = "使命描述", example = "通过技术文章分享，帮助开发者解决实际问题，共同成长")
    @TableField("mission_description")
    private String missionDescription;

    @ApiModelProperty(value = "具体使命要点", example = "1. 输出高质量技术文章；2. 解答读者问题；3. 分享实战经验")
    @TableField("mission_point_list_str")
    private String missionPointListStr;
}