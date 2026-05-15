package cn.pzhdv.blog.help;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.IFill;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.fill.Column;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MyBatis-Plus 代码生成器
 *
 * <p>功能说明：
 * <ul>
 *   <li>自动生成 Entity、Mapper、Service、Controller 等代码</li>
 *   <li>支持乐观锁、逻辑删除、字段自动填充等特性</li>
 *   <li>支持 Swagger 文档注解生成</li>
 *   <li>支持 Lombok 简化代码</li>
 * </ul>
 *
 * <p>使用步骤：
 * <ol>
 *   <li>修改数据库连接配置（url、username、password）</li>
 *   <li>修改包名配置（parentPackage）</li>
 *   <li>设置要生成的表名（tableNames）</li>
 *   <li>运行 main 方法即可生成代码</li>
 * </ol>
 *
 * <p>注意事项：
 * <ul>
 *   <li>生成前请确认数据库连接信息正确</li>
 *   <li>如需覆盖已存在的文件，请将 FILE_OVERRIDE 设置为 true</li>
 *   <li>生成的代码需要手动添加 @JsonFormat 等注解到时间字段</li>
 *   <li>乐观锁字段需要在实体类上添加 @Version 注解</li>
 * </ul>
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-06-25 13:10:16
 */
public class MPCodeGenerator {

    // ==================== 数据库配置 ====================
    /**
     * 数据库连接URL
     */
    private static final String URL = "jdbc:mysql://localhost:3306/pzh_blog?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC";

    /**
     * 数据库用户名
     */
    private static final String USERNAME = "root";

    /**
     * 数据库密码
     */
    private static final String PASSWORD = "root";

    // ==================== 代码生成配置 ====================
    /**
     * 作者名称
     */
    private static final String AUTHOR_NAME = "PanZonghui";

    /**
     * 父包名
     */
    private static final String PARENT_PACKAGE = "cn.pzhdv.blog";

    /**
     * 表前缀，配置后生成的代码不会有此前缀（例如：sys_ -> 生成时去掉 sys_）
     */
    private static final String[] TABLE_PREFIX = {};

    /**
     * 要生成的表名，空数组 {} 表示生成所有表
     */
    private static final String[] TABLE_NAMES = {"article_category","article_tag","article","article_category_relation","article_tag_relation","blog_author","blog_mission","job_experience","sys_operation_log"};

    /**
     * 是否覆盖已存在的文件（true: 覆盖, false: 跳过）
     */
    private static final boolean FILE_OVERRIDE = false;

    // ==================== 特殊字段配置 ====================
    /**
     * 乐观锁字段名（版本号，用于乐观锁，数据库中默认值设置为: 1）
     */
    private static final String VERSION_COLUMN_NAME = "version";

    /**
     * 逻辑删除字段名（0:未删除 1:已删除，数据库中默认值设置为: 0）
     */
    private static final String LOGIC_DELETE_COLUMN_NAME = "deleted";

    /**
     * 创建时间字段名
     */
    private static final String CREATE_TIME_COLUMN_NAME = "create_time";

    /**
     * 更新时间字段名
     */
    private static final String UPDATE_TIME_COLUMN_NAME = "update_time";

    // ==================== 自动填充字段配置 ====================
    /**
     * 自定义需要自动填充的字段列表
     */
    private static final List<IFill> COLUMN_FILL_LIST = new ArrayList<>();

    static {
        // 初始化自动填充字段
        COLUMN_FILL_LIST.add(new Column(VERSION_COLUMN_NAME, FieldFill.INSERT));
        COLUMN_FILL_LIST.add(new Column(LOGIC_DELETE_COLUMN_NAME, FieldFill.INSERT));
        COLUMN_FILL_LIST.add(new Column(CREATE_TIME_COLUMN_NAME, FieldFill.INSERT));
        COLUMN_FILL_LIST.add(new Column(UPDATE_TIME_COLUMN_NAME, FieldFill.INSERT_UPDATE));
    }

    /**
     * 实体类特殊字段使用示例
     *
     * <p>在生成的实体类中，需要手动添加以下注解：
     *
     * <pre>{@code
     * // 创建时间字段示例
     * @ApiModelProperty(value = "创建时间")
     * @TableField(value = "create_time", fill = FieldFill.INSERT)
     * @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
     * private Date createTime;
     *
     * // 更新时间字段示例
     * @ApiModelProperty(value = "更新时间")
     * @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
     * @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
     * private Date updateTime;
     *
     * // 乐观锁字段示例（使用 @JsonIgnore 注解，忽略此属性，前端不会拿到该属性）
     * @JsonIgnore
     * @ApiModelProperty("版本号")
     * @TableField(value = "version", fill = FieldFill.INSERT)
     * @Version
     * private Integer version;
     *
     * // 逻辑删除字段示例
     * @JsonIgnore //（使用 @JsonIgnore 注解，忽略此属性，前端不会拿到该属性）
     * @TableLogic
     * @TableField(value = "deleted", fill = FieldFill.INSERT)
     * private Byte deleted;
     * }</pre>
     */

    /**
     * 主方法：执行代码生成
     *
     * @param args 命令行参数（暂未使用）
     */
    public static void main(String[] args) {
        try {
            System.out.println("==========================================");
            System.out.println("    MyBatis-Plus 代码生成器启动");
            System.out.println("==========================================");
            System.out.println("数据库连接: " + URL);
            System.out.println("生成表名: " + String.join(", ", TABLE_NAMES));
            System.out.println("父包名: " + PARENT_PACKAGE);
            System.out.println("覆盖模式: " + (FILE_OVERRIDE ? "开启（将覆盖已存在的文件）" : "关闭（跳过已存在的文件）"));
            System.out.println("==========================================");

            // 如果开启覆盖模式，先删除已存在的文件
            if (FILE_OVERRIDE) {
                deleteExistingFiles();
            }

            FastAutoGenerator.create(URL, USERNAME, PASSWORD)
                    // ==================== 全局配置 ====================
                    .globalConfig(builder -> {
                        builder
                                .disableOpenDir()  // 禁止打开输出目录
                                .author(AUTHOR_NAME) // 生成的作者名字
                                .enableSwagger() // 开启 Swagger 注解
                                .dateType(DateType.ONLY_DATE)   // 定义生成的实体类中日期类型
                                // DateType.ONLY_DATE: 使用 java.util.Date
                                // DateType.TIME_PACK: 使用 java.time 包下的时间类型（推荐）
                                .commentDate("yyyy-MM-dd HH:mm:ss") // 注释日期格式
                                .outputDir(getOutputDir()); // 指定输出目录
                    })
                    // ==================== 包配置 ====================
                    .packageConfig(builder -> {
                        builder
                                .parent(PARENT_PACKAGE) // 父包名
                                .entity("entity") // 实体类包名
                                .controller("controller") // 控制层包名
                                .service("service") // Service 层包名
                                .serviceImpl("service.impl") // Service 实现类包名
                                .mapper("mapper") // Mapper 层包名
                                // .moduleName("module") // 设置父包模块名（可选）
                                // 设置 Mapper XML 生成路径
                                .pathInfo(Collections.singletonMap(
                                        OutputFile.xml,
                                        getMapperXmlOutputDir()
                                ));
                    })
                    // ==================== 策略配置 ====================
                    .strategyConfig(builder -> {
                        builder
                                // 设置要生成的表名
                                .addInclude(TABLE_NAMES)
                                // 表名前缀，配置后生成的代码不会有此前缀
                                .addTablePrefix(TABLE_PREFIX)

                                // ========== Entity 策略配置 ==========
                                .entityBuilder()
                                .formatFileName("%s") // 格式化实体名称，%s 表示表名（驼峰命名）
                                .enableLombok() // 实体类使用 Lombok（需要引入依赖）
                                .enableChainModel() // 开启链式模型
                                .enableRemoveIsPrefix() // 移除 Boolean 类型字段的 is 前缀
                                .enableTableFieldAnnotation() // 属性加上 @TableField 注解说明
                                .naming(NamingStrategy.underline_to_camel) // 数据表映射实体命名策略：下划线转驼峰
                                .columnNaming(NamingStrategy.underline_to_camel) // 表字段映射实体属性命名规则：下划线转驼峰
                                .idType(IdType.AUTO) // 主键策略
                                // IdType.AUTO: 数据库自增
                                // IdType.ASSIGN_ID: 雪花算法自动生成的 ID（推荐分布式系统）
                                // IdType.ASSIGN_UUID: UUID
                                // IdType.NONE: 无状态
                                .versionColumnName(VERSION_COLUMN_NAME) // 乐观锁字段名
                                .logicDeleteColumnName(LOGIC_DELETE_COLUMN_NAME) // 逻辑删除字段名
                                .addTableFills(COLUMN_FILL_LIST) // 自动填充配置

                                // ========== Controller 策略配置 ==========
                                .controllerBuilder()
                                .formatFileName("%sController") // 控制类名称后缀
                                .enableRestStyle() // 开启 RESTful 风格
                                .enableHyphenStyle() // 开启驼峰转连字符（例如：UserInfo -> user-info）

                                // ========== Service 策略配置 ==========
                                .serviceBuilder()
                                .formatServiceFileName("%sService") // 服务层接口名后缀
                                .formatServiceImplFileName("%sServiceImpl") // 服务层实现类名后缀

                                // ========== Mapper 策略配置 ==========
                                .mapperBuilder()
                                .enableMapperAnnotation()
                                // 注意：enableMapperAnnotation() 方法已过时
                                // 推荐在启动类上使用 @MapperScan("com.example.demo.mapper") 扫描 Mapper 接口
                                .enableBaseResultMap() // 启用 XML 文件中的 BaseResultMap 生成
                                .enableBaseColumnList() // 启用 XML 文件中的 BaseColumnList 生成
                                .formatMapperFileName("%sMapper") // Mapper 接口名后缀
                                .formatXmlFileName("%sMapper"); // 格式化 XML 文件名称后缀
                    })
                    .execute();

            System.out.println("==========================================");
            System.out.println("    代码生成完成！");
            System.out.println("==========================================");

        } catch (Exception e) {
            System.err.println("代码生成失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取 Java 代码输出目录
     *
     * @return 输出目录路径
     */
    private static String getOutputDir() {
        return System.getProperty("user.dir") + "/src/main/java";
    }

    /**
     * 获取 Mapper XML 文件输出目录
     *
     * @return 输出目录路径
     */
    private static String getMapperXmlOutputDir() {
        return System.getProperty("user.dir") + "/src/main/resources/mapper";
    }

    /**
     * 删除已存在的生成文件（实现覆盖功能）
     * 删除 Entity、Mapper、Service、Controller 等已生成的文件
     */
    private static void deleteExistingFiles() {
        try {
            String basePackagePath = PARENT_PACKAGE.replace(".", "/");
            String outputDir = getOutputDir();
            String xmlOutputDir = getMapperXmlOutputDir();

            for (String tableName : TABLE_NAMES) {
                // 将表名转换为实体类名（下划线转驼峰，首字母大写）
                String entityName = toCamelCase(tableName, true);

                // 删除 Entity
                deleteFile(outputDir + "/" + basePackagePath + "/entity/" + entityName + ".java");

                // 删除 Mapper
                deleteFile(outputDir + "/" + basePackagePath + "/mapper/" + entityName + "Mapper.java");

                // 删除 Service
//                deleteFile(outputDir + "/" + basePackagePath + "/service/" + entityName + "Service.java");

                // 删除 ServiceImpl
//                deleteFile(outputDir + "/" + basePackagePath + "/service/impl/" + entityName + "ServiceImpl.java");

                // 删除 Controller
//                deleteFile(outputDir + "/" + basePackagePath + "/controller/" + entityName + "Controller.java");

                // 删除 Mapper XML
                deleteFile(xmlOutputDir + "/" + entityName + "Mapper.xml");
            }

            System.out.println("已删除已存在的文件，准备重新生成...");
        } catch (Exception e) {
            System.err.println("删除已存在文件时出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 删除指定路径的文件
     *
     * @param filePath 文件路径
     */
    private static void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                System.out.println("已删除: " + filePath);
            }
        } catch (IOException e) {
            System.err.println("删除文件失败: " + filePath + " - " + e.getMessage());
        }
    }

    /**
     * 将下划线命名转换为驼峰命名
     *
     * @param str                  下划线命名字符串
     * @param firstLetterUpperCase 首字母是否大写
     * @return 驼峰命名字符串
     */
    private static String toCamelCase(String str, boolean firstLetterUpperCase) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        // 移除表前缀
        String name = str;
        for (String prefix : TABLE_PREFIX) {
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
                break;
            }
        }

        // 下划线转驼峰
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = firstLetterUpperCase;

        for (char c : name.toCharArray()) {
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }
}
