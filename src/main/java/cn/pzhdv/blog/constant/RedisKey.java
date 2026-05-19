package cn.pzhdv.blog.constant;

/**
 * Redis Key 常量定义
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
public class RedisKey {

    /**
     * 缓存过期时间：基础值（30分钟）+ 随机偏移量（0-10分钟），单位：秒
     */
    public static final int BASE_EXPIRE_SECONDS = 30 * 60;
    // ======================== 全局缓存过期配置 ========================
    public static final int RANDOM_EXPIRE_SECONDS = 10 * 60;
    // ======================== 后台管理模块 ========================
    // 系统用户
    public static final String SYSTEM_USER_DETAIL_KEY = "system:user:detail:";
    public static final String SYSTEM_USER_PAGE_KEY = "system:user:page:";
    public static final String SYSTEM_USER_USERNAME_EXISTS_KEY = "system:user:username:exists:";
    public static final String SYSTEM_USER_EMAIL_EXISTS_KEY = "system:user:email:exists:";
    public static final String SYSTEM_USER_PHONE_EXISTS_KEY = "system:user:phone:exists:";
    // 系统角色
    public static final String SYS_ROLE_PAGE_KEY = "sys:role:page:";
    public static final String SYS_ROLE_ALL_KEY = "sys:role:all";
    public static final String SYS_ROLE_CODE_EXISTS_KEY = "sys:role:code:exists:";
    // 按钮权限
    public static final String SYS_BUTTON_PAGE_KEY = "sys:button:page:";
    public static final String SYS_BUTTON_LIST_KEY = "sys:button:list:";
    public static final String SYS_BUTTON_DETAIL_KEY = "sys:button:detail:";
    public static final String SYS_BUTTON_CODE_EXISTS_KEY = "sys:button:code:exists:";
    // 系统菜单
    public static final String SYS_MENU_PAGE_KEY = "sys:menu:page:";
    public static final String SYS_MENU_TREE_KEY = "sys:menu:tree";
    public static final String SYS_MENU_ALL_PAGES_KEY = "sys:menu:all:pages";
    public static final String SYS_MENU_DETAIL_KEY = "sys:menu:detail:";
    // ======================== 博客业务公共模块 ========================
    // 文章分类
    public static final String ARTICLE_CATEGORY_TREE_LIST_KEY = "article:category:tree:list";
    public static final String ARTICLE_CATEGORY_TOTAL_KEY = "article:category:total";
    public static final String ARTICLE_CATEGORY_LIST_KEY = "article:category:list";
    // 文章标签
    public static final String ARTICLE_TAG_CACHE_KEY = "article:tag:cache";
    public static final String ARTICLE_TAG_TOTAL_KEY = "article:tag:total";
    // 文章通用
    public static final String ARTICLE_CONDITION_PAGE_LIST_KEY = "article:condition:page:list";
    public static final String ARTICLE_TOTAL_KEY = "article:total";
    public static final String ARTICLE_PUBLISH_DATE_LIST_KEY = "article:publish:date:list";
    public static final String ARTICLE_HOME_PAGE_LIST_KEY = "article:home:page:list";
    public static final String ARTICLE_CATEGORY_PAGE_LIST_KEY = "article:category:page:list";
    // 文章详情 markdown
    public static final String ARTICLE_DETAIL_MARKDOWN_KEY = "article:detail:markdown:";
    // 文章详情 html
    public static final String ARTICLE_DETAIL_HTML_KEY = "article:detail:html:";
    // 博主/履历信息
    public static final String BLOG_AUTHOR_CACHE_KEY = "blog:author:cache";
    public static final String BLOG_MISSION_CACHE_KEY = "blog:mission:cache";
    public static final String JOB_EXPERIENCE_CACHE_KEY = "job:experience:cache";
    private RedisKey() {
        // 私有构造 禁止实例化
    }

}