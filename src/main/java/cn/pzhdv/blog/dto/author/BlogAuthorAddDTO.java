package cn.pzhdv.blog.dto.author;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 博主信息新增DTO
 *
 * @author PanZonghui
 * @since 2026-05-11
 */
@Data
@ApiModel(value = "BlogAuthorAddDTO", description = "博主信息新增请求参数")
public class BlogAuthorAddDTO {

    @NotBlank(message = "姓名不能为空")
    @Length(min = 1, max = 20, message = "姓名长度需在1-20个字符之间")
    @ApiModelProperty(value = "姓名（必填，1-20个字符）", required = true, example = "张三")
    private String fullName;

    @NotBlank(message = "用户昵称不能为空")
    @Length(min = 1, max = 30, message = "用户昵称长度需在1-30个字符之间")
    @ApiModelProperty(value = "用户昵称（必填，1-30个字符，用于展示）", required = true, example = "编程爱好者")
    private String userNick;

    @Length(max = 255, message = "头像URL长度不能超过255个字符")
    @URL(message = "头像需为有效的URL格式")
    @ApiModelProperty(value = "用户头像（可选，需为URL路径，最大255字符）", example = "https://xxx.com/avatar.jpg")
    private String avatar;

    @Length(min = 1, max = 50, message = "职位长度需在1-50个字符之间")
    @ApiModelProperty(value = "职位（可选，1-50个字符）", example = "Java开发工程师")
    private String position;

    @Length(min = 1, max = 500, message = "个人简介长度需在1-500个字符之间")
    @ApiModelProperty(value = "个人简介（可选，1-500个字符）", example = "专注于后端开发，热爱技术分享...")
    private String selfIntroduction;

    @Email(message = "邮箱格式不正确")
    @ApiModelProperty(value = "个人邮箱（可选，需符合邮箱格式）", example = "example@xxx.com")
    private String email;

    @Length(max = 255, message = "个人站点URL长度不能超过255个字符")
    @URL(message = "个人站点需为有效的URL格式")
    @ApiModelProperty(value = "个人站点（可选，需为URL格式）", example = "https://blog.xxx.com")
    private String website;

    @Length(max = 255, message = "GitHub链接长度不能超过255个字符")
    @URL(message = "GitHub链接需为有效的URL格式")
    @ApiModelProperty(value = "GitHub链接（可选，需为URL格式）", example = "https://github.com/xxx")
    private String github;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确（需为11位有效号码）")
    @ApiModelProperty(value = "联系电话（可选，需为11位手机号）", example = "13800138000")
    private String phone;

    @ApiModelProperty(value = "学历（可选，1-20个字符）", example = "本科")
    private String educationLevel;

    @Length(min = 1, max = 50, message = "学校名称长度需在1-50个字符之间")
    @ApiModelProperty(value = "学校名称（可选，1-50个字符）", example = "北京大学")
    private String schoolName;

}
