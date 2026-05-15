package cn.pzhdv.blog.config;

import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;

/**
 * Knife4j接口文档配置类（增强版Swagger）
 * <p>
 * 核心功能：
 * 1. 基于Swagger/OAS 3.0规范构建接口文档，适配Knife4j增强UI；
 * 2. 支持通过YML配置文件动态注入文档基础信息（标题、版本、扫描包等）；
 * 3. 配置JWT令牌全局鉴权，支持在文档页面携带Token调试接口；
 * 4. 支持开关控制（enableSwagger），适配开发/生产环境动态启停文档。
 * <p>
 * 访问地址：http://{部署IP}:{服务端口}/doc.html
 * 配置说明：
 * - 基础信息（标题/版本/联系人）：通过swagger.config.*配置项注入；
 * - JWT令牌头：通过jwt.header配置项指定（默认Authorization）；
 * - 扫描范围：通过swagger.config.basePackage指定需生成文档的接口包路径。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 * @see <a href="https://doc.xiaominfo.com/docs/knife4j">Knife4j官方文档</a>
 */
@Configuration
public class Knife4jConfig {

    @Value("${swagger.config.hostName}")
    private String hostName;
    @Value("${swagger.config.groupName}")
    private String groupName;
    @Value("${swagger.config.title}")
    private String title;
    @Value("${swagger.config.description}")
    private String description;
    @Value("${swagger.config.name}")
    private String name;
    @Value("${swagger.config.url}")
    private String url;
    @Value("${swagger.config.email}")
    private String email;
    @Value("${swagger.config.version}")
    private String version;
    @Value("${swagger.config.basePackage}")
    private String basePackage;
    @Value("${swagger.config.enableSwagger}")
    private boolean enableSwagger;
    @Value("${swagger.config.serviceUrl}")
    private String serviceUrl;
    @Value("${jwt.header}")
    private String authorizationHeaderName;

    /**
     * 创建该API的基本信息（这些基本信息会展现在文档页面中）
     * knife4j接口文档路径:http://ip:port/doc.html
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()//
                .title(title)   //文档标题
                .description(description)    //文档描述
                .version(version) //版本号
                .contact(new Contact(name, url, email)) // 联系人信息
                .termsOfServiceUrl(serviceUrl) // 组织链接
//                .license("swagger-的使用(详细教程)") // 许可证
//                .licenseUrl("https://blog.csdn.net/xhmico/article/details/125353535") // 许可证链接
                .build();
    }

    /**
     * 安全模式，这里指定token通过Authorization头请求头传递
     */
    private List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> apiKeyList = new ArrayList<SecurityScheme>();
        apiKeyList.add(new ApiKey(authorizationHeaderName, authorizationHeaderName,  In.HEADER.toValue()));
        return apiKeyList;
    }

    /**
     * 安全上下文
     */
    private List<SecurityContext> securityContexts() {
        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(
                SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        // 声明作用域
                        .operationSelector(o -> o.requestMappingPattern().matches("/.*"))
                        .build());
        return securityContexts;
    }

    /**
     * 默认的全局鉴权策略
     */
    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> securityReferences = new ArrayList<>();
        securityReferences.add(new SecurityReference(authorizationHeaderName, authorizationScopes));
        return securityReferences;
    }


    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .host(hostName)// 主机名
                .enable(enableSwagger) // 定义是否开启swagger，false为关闭，可以通过变量控制
                .apiInfo(apiInfo()) // apiInfo()：配置 API 的一些基本信息，比如：文档标题title，文档描述description，文档版本号version
                .groupName(groupName)
                .select() // select()：生成 API 文档的选择器，用于指定要生成哪些 API 文档
                .apis(RequestHandlerSelectors.basePackage(basePackage)) // apis()：指定要生成哪个包下的 API 文档
                .paths(PathSelectors.any())// paths()：指定要生成哪个 URL 匹配模式下的 API 文档。这里使用 PathSelectors.any()，表示生成所有的 API 文档。
                .build()

                /* 设置安全模式，swagger可以设置访问token */
                .securitySchemes(securitySchemes())// 安全模式
                .securityContexts(securityContexts())// 安全上下文
                .pathMapping("/");
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        PropertySourcesPlaceholderConfigurer c = new PropertySourcesPlaceholderConfigurer();
        c.setIgnoreUnresolvablePlaceholders(true);
        return c;
    }
}
