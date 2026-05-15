package cn.pzhdv.blog.context;

import cn.pzhdv.blog.entity.SystemUser;

/**
 * 当前登录用户上下文
 * 基于 ThreadLocal 实现线程隔离
 * 每个请求线程独立用户信息，请求结束必须清理，防止线程复用串号、内存泄漏
 *
 * @author PanZonghui
 */
public class UserContext {

    /**
     * 线程本地存储：存放当前请求登录用户
     */
    private static final ThreadLocal<SystemUser> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前登录用户
     * @param user 登录用户实体
     */
    public static void setUser(SystemUser user) {
        if (user != null) {
            USER_THREAD_LOCAL.set(user);
        }
    }

    /**
     * 获取当前登录用户
     * @return 登录用户 / null（未登录）
     */
    public static SystemUser getUser() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 获取当前登录用户名
     * 未登录返回 anonymous 匿名标识
     * @return 用户名
     */
    public static String getUsername() {
        SystemUser user = getUser();
        return user == null ? "anonymous" : user.getUserName();
    }

    /**
     * 清空当前线程用户信息
     * 在拦截器 afterCompletion 中调用，必加
     */
    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }
}