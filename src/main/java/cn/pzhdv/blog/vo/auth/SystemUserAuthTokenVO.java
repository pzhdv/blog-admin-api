package cn.pzhdv.blog.vo.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 系统用户登录接口返回模型
 * 封装令牌信息和用户基本信息，屏蔽敏感字段（如密码）
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "SystemUserAuthTokenVO", description = "登录成功后返回的令牌及用户信息")
public class SystemUserAuthTokenVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "访问令牌（用于接口鉴权）", required = true,
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @ApiModelProperty(value = "刷新令牌（用于获取新的访问令牌）", required = true,
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
}
