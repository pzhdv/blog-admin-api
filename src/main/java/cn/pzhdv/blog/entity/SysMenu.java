package cn.pzhdv.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 系统菜单表
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02 23:03:05
 */
@Getter
@Setter
@ToString
@TableName("sys_menu")
@Accessors(chain = true)
@ApiModel(value = "SysMenu对象", description = "系统菜单表")
public class SysMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 子菜单列表（树形结构）
     */
    @ApiModelProperty(value = "当前菜单的子菜单列表", example = "[]", required = false)
    @TableField(exist = false)
    List<SysMenu> children;

    /**
     * 用户是否有该菜单权限
     */
    @ApiModelProperty(value = "用户是否有该菜单权限", example = "true", required = false)
    @TableField(exist = false)
    private Boolean hasPermission;
    /**
     * 菜单ID（主键）
     */
    @ApiModelProperty("菜单ID（主键）")
    @TableId(value = "menu_id", type = IdType.AUTO)
    private Long menuId;
    /**
     * 菜单类型：1=目录，2=菜单（枚举值不可修改）
     */
    @NotNull(message = "菜单类型不能为空")
    @Min(value = 1, message = "菜单类型必须为1（目录）或2（菜单）")
    @Max(value = 2, message = "菜单类型必须为1（目录）或2（菜单）")
    @TableField("menu_type")
    @ApiModelProperty("菜单类型：1=目录，2=菜单（枚举值不可修改）")
    private Byte menuType;
    /**
     * 菜单名称（如：系统管理,用户管理）
     */
    @NotBlank(message = "菜单名称不能为空")
    @TableField("menu_name")
    @ApiModelProperty("菜单名称（如：系统管理,用户管理）")
    private String menuName;
    /**
     * 路由名称(如：manage,manage_user,manage_user_[id])
     */
    @NotBlank(message = "路由名称不能为空")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9_\\-\\[\\]]*$",
            message = "路由名称只能包含字母、数字、下划线、连字符、中括号，且必须以字母开头"
    )
    @TableField("route_name")
    @ApiModelProperty("路由名称(如：manage,manage_user,manage_user_[id])")
    private String routeName;
    /**
     * 路由路径（如：/manage,/manage/user）
     */
    @NotBlank(message = "路由路径不能为空")
    @Pattern(regexp = "^/[a-zA-Z0-9_/\\-:]*$", message = "路由路径必须以/开头，只能包含字母、数字、下划线、斜杠、横线和冒号")
    @TableField("route_path")
    @ApiModelProperty("路由路径（如：/manage,/manage/user）")
    private String routePath;

    /**
     * 组件路径（如：layout.base$view.home、view.manage_user，空表示无组件；允许$符号）
     */
    @TableField("component")
    @ApiModelProperty("组件路径（如：layout.base$view.home、view.manage_user，空表示无组件；允许$符号）")
    private String component;
    /**
     * 布局类型（如：\"(base)\"、\"(blank)\"，允许()符号）
     */
    @TableField("layout")
    @ApiModelProperty("布局类型（如：\"(base)\"、\"(blank)\"，允许()符号）")
    private String layout;
    /**
     * 国际化key（对应语言包）
     */
    @TableField("i18n_key")
    @ApiModelProperty("国际化key（对应语言包）")
    private String i18nKey;
    /**
     * 菜单排序（数字越小越靠前）
     */
    @TableField("`order`")
    @ApiModelProperty("菜单排序（数字越小越靠前）")
    private Short order;
    /**
     * 图标类型：1=iconify图标，2=本地图标（枚举值不可修改）
     */
    @TableField("icon_type")
    @ApiModelProperty("图标类型：1=iconify图标，2=本地图标（枚举值不可修改）")
    private Byte iconType;
    /**
     * 图标标识（如：ep:menu）
     */
    @TableField("icon")
    @ApiModelProperty("图标标识（如：ep:menu）")
    private String icon;
    /**
     * 菜单状态：1=启用，2=禁用（枚举值不可修改）
     */
    @TableField("status")
    @ApiModelProperty("菜单状态：1=启用，2=禁用（枚举值不可修改）")
    private Byte status;
    /**
     * 缓存路由：false=否，true=是（对应数据库0/1）
     */
    @TableField("keep_alive")
    @ApiModelProperty("缓存路由：false=否，true=是")
    private Boolean keepAlive;
    /**
     * 常量路由：false=否，true=是（对应数据库0/1）
     */
    @TableField("constant")
    @ApiModelProperty("常量路由：false=否，true=是")
    private Boolean constant;
    /**
     * 外链地址（http/https开头）
     */
    @TableField("href")
    @ApiModelProperty("外链地址（http/https开头）")
    private String href;
    /**
     * 隐藏菜单：false=否，true=是（对应数据库0/1）
     */
    @TableField("hide_in_menu")
    @ApiModelProperty("隐藏菜单：false=否，true=是")
    private Boolean hideInMenu;
    /**
     * 父级菜单ID（0表示根菜单）
     */
    @TableField("parent_id")
    @ApiModelProperty("父级菜单ID（0表示根菜单）")
    private Long parentId;

    /**
     * 高亮的菜单（指定menu_id，用于非菜单页高亮）
     */
    @TableField("active_menu")
    @ApiModelProperty("高亮的菜单（指定menu_id，用于非菜单页高亮）")
    private String activeMenu;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间（自动填充，无需传值）", example = "2025-06-25 10:30:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "修改时间（自动填充，无需传值）", example = "2025-06-25 11:45:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
    /**
     * 支持多页签：false=否，true=是（对应数据库0/1）
     */
    @TableField("multi_tab")
    @ApiModelProperty("支持多页签：false=否，true=是")
    private Boolean multiTab;
    /**
     * 固定在页签中的序号
     */
    @TableField("fixed_index_in_tab")
    @ApiModelProperty("固定在页签中的序号")
    private Short fixedIndexInTab;
    /**
     * 路由参数（JSON格式，示例：[{\"key\":\"age\",\"value\":\"18\"},{\"key\":\"id\",\"value\":\"1001\"}]，仅允许合法JSON字符串）
     */
    @TableField("query")
    @ApiModelProperty("路由参数（JSON格式，示例：[{\"key\":\"age\",\"value\":\"18\"},{\"key\":\"id\",\"value\":\"1001\"}]，仅允许合法JSON字符串）")
    private String query;
}