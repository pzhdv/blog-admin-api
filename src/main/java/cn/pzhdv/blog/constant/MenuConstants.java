package cn.pzhdv.blog.constant;

/**
 * 菜单相关常量
 *
 * @author PanZonghui
 * @since 2026-01-02
 */
public class MenuConstants {

    public static final String ROUTE_PREFIX_DOCUMENT = "document"; // 文档路由前缀
    public static final String ROUTE_PREFIX_EXCEPTION = "exception"; // 异常路由前缀
    public static final String COMPONENT_IFRAME_PAGE = "page.iframe-page"; // 文档菜单固定值
    public static final String COMPONENT_NORMAL_PREFIX = "page.(base)_"; // 普通路由前缀

    /**
     * 根菜单的父级ID
     */
    public static final Long ROOT_PARENT_ID = 0L;

    /**
     * 未删除状态
     */
    public static final Byte NOT_DELETED = 0;

    /**
     * 菜单类型：目录
     */
    public static final Byte MENU_TYPE_DIRECTORY = 1;

    /**
     * 菜单类型：菜单
     */
    public static final Byte MENU_TYPE_MENU = 2;

    /**
     * 私有构造函数，防止实例化
     */
    private MenuConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}

