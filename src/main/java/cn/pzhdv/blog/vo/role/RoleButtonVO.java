package cn.pzhdv.blog.vo.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 角色按钮信息VO
 * </p>
 * <p>
 * 用于返回角色拥有的按钮列表
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02
 */
@Data
@ApiModel(value = "RoleButtonVO", description = "角色按钮信息VO")
public class RoleButtonVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    @Min(value = 1, message = "角色ID必须为正整数")
    @ApiModelProperty(value = "角色ID", required = true, example = "1")
    private Long roleId;

    /**
     * 按钮Id列表
     */
    @ApiModelProperty(value = "按钮Id列表", example = "[1, 2, 3]")
    private List<Long> buttonIdList;

}

