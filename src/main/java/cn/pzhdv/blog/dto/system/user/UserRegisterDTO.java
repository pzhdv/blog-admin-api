package cn.pzhdv.blog.dto.system.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;
import java.util.List;

/**
 * 用户注册DTO（仅包含注册所需字段）
 */
@Data
@ApiModel(value = "UserRegisterDTO", description = "用户注册请求参数")
public class UserRegisterDTO {


    /**
     * 用户名（注册必传，唯一，4-20位字母/数字/下划线）
     */
    @ApiModelProperty(
            value = "用户名（必须传递，唯一，4-20位字母/数字/下划线）",
            example = "admin123",
            required = true
    )
    @NotBlank(message = "用户名不能为空")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]{4,20}$",
            message = "用户名需为4-20位字母、数字或下划线"
    )
    private String userName;

    /**
     * 登录密码（注册必传，8-32位，包含字母+数字+特殊字符）
     */
    @ApiModelProperty(
            value = "登录密码（必须传递，8-32位，包含字母+数字+特殊字符）",
            example = "Admin@123456",
            required = true
    )
    @NotBlank(message = "登录密码不能为空")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,32}$",
            message = "密码需8-32位，包含字母、数字和特殊字符"
    )
    private String password;

    /**
     * 用户昵称（必须传递）
     */
    @ApiModelProperty(value = "用户昵称（必须传递，2-10位中文/字母/数字）", example = "三碗小鱼干", required = true)
    @NotBlank(message = "用户昵称不能为空")
    @Pattern(
            regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9]{2,10}$",
            message = "用户昵称需2-10位中文、字母或数字"
    )
    private String userNick;

    /**
     * 手机号（必须传递）
     */
    @ApiModelProperty(value = "手机号（必须传递，11位纯数字）", example = "13800138000", required = true)
    @NotBlank(message = "手机号不能为空")
    @Pattern(
            regexp = "^1[3-9]\\d{9}$",
            message = "请输入有效的11位手机号"
    )
    private String userPhone;

    /**
     * 用户邮箱（必须传递）
     */
    @ApiModelProperty(value = "用户邮箱（必须传递，格式需合法）", example = "admin@example.com", required = true)
    @NotBlank(message = "用户邮箱不能为空")
    @Email(message = "请输入有效的邮箱地址")
    private String userEmail;

    /**
     * 用户性别（必须传递）
     */
    @ApiModelProperty(
            value = "用户性别（必须传递，1:男 2:女）",
            example = "1",  // Swagger示例仍用字符串（前端展示），不影响后端接收
            required = true
    )
    @NotNull(message = "用户性别不能为空")  // 替换@NotBlank：校验Byte非null（数值类型无“空白”概念）
    @Min(value = 1, message = "性别只能是1（男）或2（女）")  // 最小值限制为1
    @Max(value = 2, message = "性别只能是1（男）或2（女）")  // 最大值限制为2
    private Byte userGender;

    /**
     * 用户状态（必须传递）
     */
    @ApiModelProperty(
            value = "用户状态（必须传递，1:正常 2:禁止）",
            example = "1",
            required = true
    )
    @NotNull(message = "用户状态不能为空")  // 替换 @NotBlank：校验数值非 null
    @Min(value = 1, message = "状态只能是1（正常）或2（禁止）")  // 最小值 1
    @Max(value = 2, message = "状态只能是1（正常）或2（禁止）")  // 最大值 2
    private Byte status;

    /**
     * 头像 头像URL（可选，需为有效URL）
     */
    @ApiModelProperty(value = "头像URL（可选更新，需为有效URL）", example = "https://example.com/avatar.png")
    @URL(message = "头像需为有效的URL格式")
    @Size(max = 255, message = "头像URL长度不能超过255个字符")
    private String avatar;

    /**
     * 用户关联的角色ID列表（注册时可选绑定，若必填则添加@NotEmpty）
     */
    @ApiModelProperty(value = "用户关联的角色ID列表（注册时可选绑定）",
            example = "[1,2,3]", required = false)
    // 若注册必须绑定角色，取消下面注释
    // @NotEmpty(message = "至少绑定一个角色")
    private List<Long> roleIds;
}
