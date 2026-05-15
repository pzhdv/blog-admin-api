package cn.pzhdv.blog.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * MyBatis-Plus 实体字段自动填充配置类
 * <p>
 * 核心功能：
 * 1. 实现MetaObjectHandler接口，拦截MyBatis-Plus的CRUD操作，自动填充通用字段；
 * 2. 插入操作（INSERT）：自动填充创建时间、更新时间、逻辑删除标记、乐观锁版本号；
 * 3. 更新操作（UPDATE）：自动填充更新时间；
 * 4. 统一管理通用字段的填充规则，避免业务代码重复设置这些字段。
 * <p>
 * 适用字段规范：
 * - createTime：创建时间（仅插入时填充）；
 * - updateTime：更新时间（插入/更新时均填充）；
 * - deleted：逻辑删除标记（默认0=未删除，需配合@TableLogic注解）；
 * - version：乐观锁版本号（默认1，需配合@Version注解）。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 * @see <a href="https://baomidou.com/pages/4c6bcf/">MyBatis-Plus 自动填充官方文档</a>
 */
@Component
public class EntityMetaHandler implements MetaObjectHandler {

    /**
     * 插入操作字段自动填充（INSERT）
     * <p>
     * 填充规则：
     * 1. createTime：设置为当前系统时间（仅首次插入时填充）；
     * 2. updateTime：设置为当前系统时间（与创建时间保持一致）；
     * 3. deleted：默认填充0（表示未删除，逻辑删除初始值）；
     * 4. version：默认填充1（乐观锁初始版本号）。
     * <p>
     * 注意事项：
     * - 实体类对应字段需添加@TableField(fill = FieldFill.INSERT)注解，标记为插入时填充；
     * - 字段名需与方法中指定的属性名（如"createTime"）严格一致（驼峰命名）；
     * - 严格填充模式（strictInsertFill）：仅当字段值为null时才填充，避免覆盖手动设置的值。
     *
     * @param metaObject MyBatis元对象，用于操作实体字段值
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        /**
         * 实体类字段配置示例（需添加对应注解）：
         *
         * // 创建时间（仅插入填充）
         * @ApiModelProperty(value = "创建时间")
         * @TableField(value = "create_time", fill = FieldFill.INSERT)
         * @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
         * private Date createTime;
         *
         * // 更新时间（插入/更新均填充）
         * @ApiModelProperty(value = "更新时间")
         * @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
         * @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
         * private Date updateTime;
         *
         * // 乐观锁版本号（仅插入填充，需配合@Version注解）
         * @ApiModelProperty("版本号,用于乐观锁")
         * @TableField(value = "version", fill = FieldFill.INSERT)
         * @Version
         * private Integer version;
         *
         * // 逻辑删除标记（仅插入填充，需配合@TableLogic注解）
         * @ApiModelProperty("逻辑删除字段(0:未删除 1:已删除)")
         * @TableField(value = "deleted", fill = FieldFill.INSERT)
         * @TableLogic
         * private Byte deleted;
         */

        // 填充创建时间（字段名：createTime，类型：Date，值：当前时间）
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        // 填充更新时间（插入时与创建时间一致）
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        // 填充逻辑删除标记（默认0=未删除）
        this.strictInsertFill(metaObject, "deleted", Byte.class, (byte) 0);
        // 填充乐观锁版本号（默认初始值1）
        this.strictInsertFill(metaObject, "version", Integer.class, 1);
    }

    /**
     * 更新操作字段自动填充（UPDATE）
     * <p>
     * 填充规则：
     * - updateTime：设置为当前系统时间（每次更新时覆盖为最新时间）；
     * <p>
     * 注意事项：
     * - 实体类updateTime字段需添加@TableField(fill = FieldFill.INSERT_UPDATE)注解；
     * - 严格更新填充模式（strictUpdateFill）：仅当字段值为null时填充，支持手动指定更新时间。
     *
     * @param metaObject MyBatis元对象，用于操作实体字段值
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充最后更新时间（字段名：updateTime，类型：Date，值：当前时间）
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}