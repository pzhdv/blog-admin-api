package cn.pzhdv.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置类
 * 支持通过YML配置文件注入跨域参数，适配不同环境（开发/测试/生产）
 * 修复通配符与allowCredentials冲突问题，优化安全性和兼容性
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Configuration
public class CrossConfig implements WebMvcConfigurer {

    /**
     * 允许跨域的源（YML注入，多个用逗号分隔）
     */
    @Value("${cors.allowed-origins:http://localhost:8080,http://localhost:9527}")
    private String allowedOrigins;

    /**
     * 允许的请求头（YML注入，多个用逗号分隔）
     */
    @Value("${cors.allowed-headers:Origin,Content-Type,Accept,Authorization,X-Requested-With,Token,Cookie}")
    private String allowedHeaders;

    /**
     * 允许的HTTP方法（YML注入，多个用逗号分隔）
     */
    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    /**
     * 是否允许携带凭证（YML注入，默认true）
     */
    @Value("${cors.allow-credentials:true}")
    private Boolean allowCredentials;

    /**
     * 暴露的响应头（YML注入，多个用逗号分隔）
     */
    @Value("${cors.exposed-headers:Authorization,Refresh-Token}")
    private String exposedHeaders;

    /**
     * 预检请求缓存时间（单位：小时，YML注入，默认1小时）
     */
    @Value("${cors.max-age:1}")
    private Long maxAge;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 1. 配置允许的源（拆分逗号分隔的字符串为列表）
        List<String> originList = Arrays.asList(allowedOrigins.split(","));
        // 兼容SpringBoot 2.4+：解决*与allowCredentials冲突问题
        if (originList.contains("*")) {
            corsConfig.setAllowedOriginPatterns(List.of("*"));
        } else {
            corsConfig.setAllowedOrigins(originList);
        }

        // 2. 配置允许的请求头
        corsConfig.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));

        // 3. 配置允许的HTTP方法
        corsConfig.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));

        // 4. 配置是否允许携带凭证
        corsConfig.setAllowCredentials(allowCredentials);

        // 5. 配置暴露的响应头
        corsConfig.setExposedHeaders(Arrays.asList(exposedHeaders.split(",")));

        // 6. 配置预检请求缓存时间
        corsConfig.setMaxAge(Duration.ofHours(maxAge));

        // 配置生效路径：所有接口
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsFilter(source);
    }
}