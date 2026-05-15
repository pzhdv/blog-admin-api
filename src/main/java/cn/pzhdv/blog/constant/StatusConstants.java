package cn.pzhdv.blog.constant;

/**
 * 状态常量类
 * <p>
 * 统一管理系统中所有实体的状态值，避免硬编码
 * </p>
 *
 * @author PanZonghui
 * @since 2026-01-02
 */
public class StatusConstants {

    /**
     * 角色状态：正常
     * 对应 SysRole.status 字段
     */
    public static final Byte ROLE_STATUS_NORMAL = 1;

    /**
     * 角色状态：禁止
     * 对应 SysRole.status 字段
     */
    public static final Byte ROLE_STATUS_FORBIDDEN = 2;

    /**
     * 菜单状态：启用
     * 对应 SysMenu.status 字段
     */
    public static final Byte MENU_STATUS_ENABLED = 1;

    /**
     * 菜单状态：禁用
     * 对应 SysMenu.status 字段
     */
    public static final Byte MENU_STATUS_DISABLED = 2;

    /**
     * 用户状态：正常
     * 对应 SystemUser.status 字段
     */
    public static final Byte USER_STATUS_NORMAL = 1;

    /**
     * 用户状态：禁止
     * 对应 SystemUser.status 字段
     */
    public static final Byte USER_STATUS_FORBIDDEN = 2;

    /**
     * 按钮状态：启用
     * 对应 SysButton.status 字段
     */
    public static final Byte BUTTON_STATUS_ENABLED = 1;

    /**
     * 按钮状态：禁用
     * 对应 SysButton.status 字段
     */
    public static final Byte BUTTON_STATUS_DISABLED = 2;

    /**
     * 私有构造函数，防止实例化
     */
    private StatusConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}

