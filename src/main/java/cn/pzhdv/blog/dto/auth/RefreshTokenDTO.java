package cn.pzhdv.blog.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 刷新令牌请求DTO
 * <p>
 * 当访问令牌（accessToken）过期时，前端通过此DTO传递刷新令牌，申请新的访问令牌，避免用户重新登录
 * </p>
 *
 * @author PanZonghui
 * @since 2025-10-17
 */
@Data
@ApiModel(value = "RefreshTokenDTO", description = "刷新令牌请求参数（用于获取新的访问令牌）")
public class RefreshTokenDTO {

    /**
     * 刷新令牌（由后端在登录时生成并返回，格式为JWT字符串）
     * <p>
     * 刷新令牌的有效期通常长于访问令牌，用于在访问令牌过期后获取新的访问令牌，
     * 避免用户频繁登录。
     * </p>
     */
    @ApiModelProperty(
            value = "刷新令牌（登录时返回的refreshToken）",
            required = true,
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            notes = "刷新令牌为登录时返回的长效令牌，用于获取新的访问令牌"
    )
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
