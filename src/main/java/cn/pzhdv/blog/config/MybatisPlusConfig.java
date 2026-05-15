package cn.pzhdv.blog.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 核心配置类
 * <p>
 * 核心功能：
 * 1. 注册MyBatis-Plus拦截器链，集成分页插件、乐观锁插件等核心增强能力；
 * 2. 适配MySQL数据库的分页逻辑，保证分页查询的准确性；
 * 3. 配置乐观锁插件，解决并发更新数据的冲突问题。
 * <p>
 * 插件说明：
 * - 分页插件（PaginationInnerInterceptor）：支持MyBatis-Plus的IPage分页查询，自动拼接分页SQL；
 * - 乐观锁插件（OptimisticLockerInnerInterceptor）：基于版本号（version字段）实现乐观锁，需实体类添加@Version注解。
 * <p>
 * 注意事项：
 * 1. 多插件配置时，分页插件需最后添加（MyBatis-Plus官方建议）；
 * 2. 分页插件指定DbType.MYSQL，适配MySQL的分页语法（LIMIT），多数据源场景可省略具体类型；
 * 3. 乐观锁插件仅对updateById/selectById+update操作生效，需保证实体类version字段为数值类型。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 * @see <a href="https://baomidou.com/pages/2976a3/">MyBatis-Plus 插件官方文档</a>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置MyBatis-Plus核心拦截器链
     * <p>
     * 拦截器加载顺序：
     * 1. 乐观锁插件（OptimisticLockerInnerInterceptor）：先处理版本号校验；
     * 2. 分页插件（PaginationInnerInterceptor）：最后处理分页逻辑（官方建议）。
     * <p>
     * 分页插件配置：
     * - 指定DbType.MYSQL：适配MySQL数据库的分页语法，避免跨数据库分页异常；
     * - 支持自动分页：Mapper层直接返回IPage对象，无需手动拼接LIMIT语句。
     *
     * @return MybatisPlusInterceptor MyBatis-Plus拦截器链实例，包含乐观锁+分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 添加乐观锁插件：解决并发更新冲突，需实体类字段添加@Version注解
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 2. 添加分页插件（最后添加）：适配MySQL分页，指定数据库类型保证兼容性
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        // 多数据源场景可省略DbType，使用无参构造：new PaginationInnerInterceptor()
        return interceptor;
    }

}