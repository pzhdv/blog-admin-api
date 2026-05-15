package cn.pzhdv.blog.vo.token;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 令牌刷新接口的返回模型 封装刷新成功后的访问令牌、刷新令牌及相关扩展字段
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "TokenRefreshVO", description = "刷新令牌成功后返回的数据模型")
public class TokenRefreshVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(
            value = "新的访问令牌",
            required = true,
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @ApiModelProperty(
            value = "新的刷新令牌",
            required = true,
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            notes = "用于下次刷新访问令牌，建议前端更新存储")
    private String refreshToken;
}
