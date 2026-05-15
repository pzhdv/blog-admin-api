package cn.pzhdv.blog.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "LoginDTO", description = "登录请求参数")
public class LoginDTO {
    /**
     * 用户名- 数据库字段：user_name
     */
    @ApiModelProperty(value = "用户名（唯一，必填，4-20位字母/数字/下划线）", example = "admin123", required = true)
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名需为4-20位字母、数字或下划线")
    private String userName;

    /**
     * 登录密码
     */
    @ApiModelProperty(value = "登录密码（必填，8-32位，包含字母+数字+特殊字符）", example = "Admin@123456", required = true)
    @NotBlank(message = "登录密码不能为空")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,32}$",
            message = "密码需8-32位，包含字母、数字和特殊字符")
    private String password;
}
