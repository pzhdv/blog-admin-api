package cn.pzhdv.blog.vo.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 通用树形结构VO
 * <p>
 * 适用于系统中所有需要返回树形结构的场景（如菜单树、部门树、权限树等），
 * 字段命名贴合前端主流树形组件（ElementUI/AntD Tree）的默认数据格式，
 * 可直接对接前端无需额外转换，提升前后端对接效率。
 * </p>
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Data
@ApiModel(description = "通用树形结构VO，适配前端树形组件默认格式")
public class TreeVO {

    /**
     * 树节点唯一标识ID
     * <p>对应业务场景中的主键（如菜单ID、部门ID、权限ID等），非空且唯一</p>
     */
    @ApiModelProperty(value = "树节点唯一标识ID（如菜单ID/部门ID）", required = true, example = "1")
    private Long value;

    /**
     * 国际化key（多语言标识）
     * <p>对应前端语言包中的key值，用于实现多语言展示；无国际化需求时可返回null/空字符串</p>
     */
    @ApiModelProperty(value = "国际化key（对应前端语言包，无则传空）", example = "menu.system.manage")
    private String i18nKey;

    /**
     * 树节点显示名称
     * <p>前端树形组件展示的节点文本（如菜单名称、部门名称、权限名称等），非空</p>
     */
    @ApiModelProperty(value = "树节点显示名称（如：系统管理、用户管理）", required = true, example = "系统管理")
    private String label;

    /**
     * 子节点列表
     * <p>当前节点的下级树形节点集合；叶子节点（无下级）时返回空列表，不可为null</p>
     */
    @ApiModelProperty(value = "子树节点列表，叶子节点返回空列表", example = "[]")
    private List<TreeVO> children;
}