package cn.pzhdv.blog.annotation;

import java.lang.annotation.*;

/**
 * 接口操作日志注解
 * <p>
 * 功能：标记需要记录操作日志的接口方法，配合 {@link cn.pzhdv.blog.aspect.ApiLogAspect} 使用
 * 使用场景：Controller 层需要记录请求、响应、操作人、IP、耗时等日志的接口
 *
 * @author PanZonghui
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiLog {

    /**
     * 接口描述（如：用户登录、新增字典、删除角色、导出数据）
     */
    String value() default "";

}