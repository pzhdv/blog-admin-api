package cn.pzhdv.blog.config;

import cn.pzhdv.blog.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 核心职责：
 * 1. 注册JWT令牌拦截器，配置拦截/放行规则，保护接口安全；
 * 2. 手动配置Knife4j/Swagger静态资源映射，解决`spring.resources.add-mappings=false`导致的文档页面404问题；
 * 3. 适配全局异常处理的404拦截逻辑，兼顾静态资源访问和接口异常捕获。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
 @Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                // 注册JWT拦截器
                .addInterceptor(jwtInterceptor)
                // 拦截所有请求路径（默认所有接口都需要认证）
                .addPathPatterns("/**")

                // ========== 放行免认证接口 ==========
                .excludePathPatterns("/auth/refreshToken") // 刷新令牌接口（令牌过期时需调用，免认证）
                .excludePathPatterns("/auth/login") // 用户登录接口（获取令牌的入口，免认证）
                .excludePathPatterns("/health/check") // 健康检查

                // ========== 放行静态资源/特殊文件 ==========
                .excludePathPatterns("/favicon.ico") // 浏览器/favicon.ico自动请求，避免拦截

                // ========== 放行Knife4j/Swagger文档相关路径 ==========
                .excludePathPatterns("/doc.html") // Knife4j UI
                .excludePathPatterns("/doc.html/**") // Knife4j UI资源
                .excludePathPatterns("/v3/api-docs/**") // OpenAPI 文档
                .excludePathPatterns("/v2/api-docs/**") // Swagger2 文档
                .excludePathPatterns("/swagger-resources/**") // Swagger 资源
                .excludePathPatterns("/webjars/**") // Swagger 静态资源
                .excludePathPatterns("/swagger-ui.html") // Swagger UI
                .excludePathPatterns("/swagger-ui/**") // Swagger UI资源
                .excludePathPatterns("/error"); // 错误页面
    }

    /**
     * 手动配置Knife4j静态资源映射
     * 因为application.yml中配置了 spring.resources.add-mappings=false 来让404走全局异常
     * 所以需要手动为Knife4j添加资源处理器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // favicon.ico 浏览器会自动请求，添加映射避免404警告
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");

        // Knife4j文档页面
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        // Swagger UI 资源
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        // Swagger资源
        registry.addResourceHandler("/swagger-resources/**")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/swagger-ui/");
    }
}
