package cn.pzhdv.blog.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云对象存储（COS）配置类
 * <p>
 * 核心功能：
 * 1. 从YML配置文件注入COS访问凭证（SecretId/SecretKey）、存储地域等核心参数；
 * 2. 初始化并配置COSClient客户端，全局单例管理，支持自动销毁（shutdown）释放资源；
 * 3. 强制启用HTTPS协议通信，保障数据传输安全性；
 * 4. 提供COSClient Bean供业务层注入，实现文件上传/下载/删除等COS操作。
 * <p>
 * 配置说明（YML配置项）：
 * - tencent.cos.secretId：腾讯云API密钥ID（从腾讯云控制台获取）；
 * - tencent.cos.secretKey：腾讯云API密钥Key（从腾讯云控制台获取，需妥善保管）；
 * - tencent.cos.region：COS存储地域（如ap-guangzhou、ap-shanghai，需与存储桶地域一致）。
 * <p>
 * 注意事项：
 * 1. 生产环境需将SecretId/SecretKey配置在环境变量或加密配置中心，避免硬编码；
 * 2. COSClient为线程安全的单例对象，无需重复创建，通过Spring容器注入使用；
 * 3. 配置destroyMethod="shutdown"，确保应用关闭时自动关闭客户端，释放连接资源。
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 * @see <a href="https://cloud.tencent.com/document/product/436/10199">腾讯云COS Java SDK官方文档</a>
 */
@Configuration
public class TencentCosConfig {

    /** 腾讯云COS API密钥ID（从YML配置注入） */
    @Value("${tencent.cos.secretId}")
    private String secretId;

    /** 腾讯云COS API密钥Key（从YML配置注入，敏感信息需加密存储） */
    @Value("${tencent.cos.secretKey}")
    private String secretKey;

    /** 腾讯云COS存储地域（如ap-guangzhou，需与存储桶地域匹配） */
    @Value("${tencent.cos.region}")
    private String region;

    /**
     * 初始化腾讯云COS客户端（全局单例Bean）
     * <p>
     * 配置项说明：
     * 1. 凭证初始化：使用BasicCOSCredentials加载SecretId/SecretKey；
     * 2. 地域配置：设置与存储桶一致的Region；
     * 3. 协议强制：启用HTTPS，防止数据传输过程中被篡改/窃取；
     * 4. 销毁策略：destroyMethod="shutdown"，应用关闭时自动关闭客户端。
     *
     * @return COSClient 腾讯云COS客户端实例，可直接注入业务层使用
     */
    @Bean(destroyMethod = "shutdown")
    public COSClient cosClient() {
        // 初始化凭证信息
        BasicCOSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 设置区域（与存储桶地域一致）
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 强制使用HTTPS协议，提升传输安全性
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // --- 性能优化配置 ---
        // 1. 设置连接超时（建立连接等待时间，单位：毫秒）
        clientConfig.setConnectionTimeout(50000);
        // 2. 设置 Socket 超时（数据传输等待时间，建议设置长一点防止大文件传输中途超时）
        clientConfig.setSocketTimeout(50000);
        // 3. 设置最大重试次数（防止由于抖动导致的偶发失败）
        clientConfig.setMaxErrorRetry(3);
        // 4. 设置连接池大小（默认 50，如果批量上传频繁，建议加大）
        clientConfig.setMaxConnectionsCount(100);
        // 创建并返回COS客户端实例
        return new COSClient(cred, clientConfig);
    }
}