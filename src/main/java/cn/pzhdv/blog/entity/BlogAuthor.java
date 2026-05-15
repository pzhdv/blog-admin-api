package cn.pzhdv.blog.entity;

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

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 用户个人信息表（存储博主/作者的基本信息）
 * </p>
 *
 * @author PanZonghui
 * @since 2025-06-25 21:03:51
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("blog_author")
@ApiModel(value = "BlogAuthor对象", description = "用户个人信息表（包含基本资料、联系方式等）")
public class BlogAuthor implements Serializable {

    private static final long serialVersionUID = 1L;

    // ------------------------------ 主键 ------------------------------
    @ApiModelProperty(value = "用户ID", example = "1001")
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    // ------------------------------ 字段 ------------------------------
    @ApiModelProperty(value = "姓名", example = "张三")
    @TableField("full_name")
    private String fullName;

    @ApiModelProperty(value = "用户昵称", example = "编程爱好者")
    @TableField("user_nick")
    private String userNick;

    @ApiModelProperty(value = "用户头像", example = "https://xxx.com/avatar.jpg")
    @TableField("avatar")
    private String avatar;

    @ApiModelProperty(value = "职位", example = "Java开发工程师")
    @TableField("position")
    private String position;

    @ApiModelProperty(value = "个人简介", example = "专注于后端开发，热爱技术分享...")
    @TableField("self_introduction")
    private String selfIntroduction;

    @ApiModelProperty(value = "个人邮箱", example = "example@xxx.com")
    @TableField("email")
    private String email;

    @ApiModelProperty(value = "个人站点", example = "https://blog.xxx.com")
    @TableField("website")
    private String website;

    @ApiModelProperty(value = "GitHub链接", example = "https://github.com/xxx")
    @TableField("github")
    private String github;

    @ApiModelProperty(value = "联系电话", example = "13800138000")
    @TableField("phone")
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "生日", example = "1990-01-01")
    @TableField("birthday")
    private Date birthday;

    @ApiModelProperty(value = "学历", example = "本科")
    @TableField("education_level")
    private String educationLevel;

    @ApiModelProperty(value = "学校名称", example = "北京大学")
    @TableField("school_name")
    private String schoolName;
}