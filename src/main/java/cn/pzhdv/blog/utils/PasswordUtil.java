package cn.pzhdv.blog.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * 密码安全工具类
 * 提供基于PBKDF2算法的密码哈希和验证功能
 * 自动管理盐值，简化密码安全处理流程（已解决Linux熵源不足导致的阻塞问题）
 */
public class PasswordUtil {
    // 加密算法：PBKDF2WithHmacSHA256（SHA-256哈希，安全性高且兼容性好）
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    // 迭代次数：100000次（平衡安全性和性能，服务器性能弱可调整为50000）
    private static final int ITERATIONS = 100000;
    // 生成密钥长度：256位（符合现代加密安全标准）
    private static final int KEY_LENGTH = 256;
    // 盐值长度：16字节（足够随机，避免盐值重复风险）
    private static final int SALT_LENGTH = 16;
    // 随机数算法：SHA1PRNG（非阻塞，解决Linux /dev/random熵源不足问题）
    private static final String RANDOM_ALGORITHM = "SHA1PRNG";

    /**
     * 生成包含盐值的完整哈希密码
     * 格式为：算法$迭代次数$盐值(Base64)$哈希值(Base64)
     * （盐值和哈希值用Base64编码，方便存储在数据库）
     * @param password 明文密码（用户输入的原始密码）
     * @return 包含盐值的哈希密码（可直接存储到数据库）
     * @throws Exception 当加密算法不可用或参数错误时抛出
     */
    public static String hashPassword(String password) throws Exception {
        // 1. 生成非阻塞的随机盐值（关键修改：解决Linux熵源不足阻塞问题）
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        // 用系统熵源初始化随机数生成器，确保盐值的随机性
        random.setSeed(random.generateSeed(SALT_LENGTH));
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt); // 生成盐值（此时不会阻塞）

        // 2. 基于PBKDF2算法计算密码哈希值（结合盐值和迭代次数）
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        // 3. 将盐值和哈希值用Base64编码（转为字符串，方便存储）
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        String encodedHash = Base64.getEncoder().encodeToString(hash);

        // 4. 拼接为标准格式的哈希字符串（便于后续验证时解析）
        return String.format("%s$%d$%s$%s", ALGORITHM, ITERATIONS, encodedSalt, encodedHash);
    }

    /**
     * 验证明文密码是否与存储的哈希密码匹配
     * @param password 明文密码（用户登录时输入的密码）
     * @param storedHash 存储的哈希密码（从数据库查询的哈希字符串）
     * @return true=密码匹配，false=密码不匹配
     * @throws Exception 当哈希格式错误、算法不可用时抛出
     */
    public static boolean verifyPassword(String password, String storedHash) throws Exception {
        // 1. 解析存储的哈希字符串（按格式拆分算法、迭代次数、盐值、哈希值）
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4) {
            throw new IllegalArgumentException("无效的哈希密码格式：需符合「算法$迭代次数$盐值$哈希值」");
        }

        // 2. 提取并转换各参数（从字符串转为对应类型）
        String algorithm = parts[0];
        int iterations = Integer.parseInt(parts[1]);
        String encodedSalt = parts[2];
        String encodedHash = parts[3];

        // 3. 验证算法一致性（防止存储的哈希算法与当前工具类算法不匹配）
        if (!ALGORITHM.equals(algorithm)) {
            throw new IllegalArgumentException("哈希算法不匹配：存储的是「" + algorithm + "」，当前支持「" + ALGORITHM + "」");
        }

        // 4. 解码盐值和原始哈希值（从Base64字符串转回字节数组）
        byte[] salt = Base64.getDecoder().decode(encodedSalt);
        byte[] originalHash = Base64.getDecoder().decode(encodedHash);

        // 5. 计算明文密码的哈希值（使用与存储时相同的盐值和迭代次数）
        byte[] testHash = pbkdf2(password.toCharArray(), salt, iterations, KEY_LENGTH);

        // 6. 固定时间比较两个哈希值（防止时序攻击，提升安全性）
        return slowEquals(originalHash, testHash);
    }

    /**
     * 核心方法：执行PBKDF2密钥派生算法，生成密码哈希值
     * @param password 明文密码（字符数组形式，比字符串更安全，可手动清空）
     * @param salt 盐值字节数组
     * @param iterations 迭代次数
     * @param keyLength 生成的密钥（哈希值）长度
     * @return 密码的哈希值字节数组
     * @throws Exception 当算法不可用或参数错误时抛出
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws Exception {
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        // 构建PBKDF2参数（包含密码、盐值、迭代次数、密钥长度）
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        // 生成密钥（本质是密码的哈希值）并返回
        return skf.generateSecret(spec).getEncoded();
    }

    /**
     * 固定时间比较两个字节数组（防止时序攻击）
     * 时序攻击：攻击者通过比较哈希值的耗时差异，推测正确的哈希值片段
     * @param a 第一个字节数组（原始哈希值）
     * @param b 第二个字节数组（待验证的哈希值）
     * @return true=两个数组完全相等，false=不相等
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        // 先比较长度，长度不同直接返回false（仍走完整循环，不泄露长度差异）
        int diff = a.length ^ b.length;
        // 遍历所有字节，即使中途发现差异也继续比较（固定耗时）
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i]; // 异或：相同为0，不同为非0
        }
        // 只有所有字节相同且长度相同时，diff才为0
        return diff == 0;
    }

//    /**
//     * 测试方法：验证密码哈希和验证流程是否正常（本地运行测试）
//     */
    public static void main(String[] args) throws Exception {
        // 模拟用户注册：明文密码
        String plainPassword = "pzh18785384970@";
        System.out.println("明文密码：" + plainPassword);

        // 1. 生成哈希密码（注册时调用，存储到数据库）
        String hashedPassword = hashPassword(plainPassword);
        System.out.println("存储到数据库的哈希密码：" + hashedPassword);

        // 2. 验证正确密码（登录时调用，匹配则登录成功）
        boolean isCorrect = verifyPassword(plainPassword, hashedPassword);
        System.out.println("正确密码验证结果：" + isCorrect); // 输出true

        // 3. 验证错误密码（登录时调用，不匹配则登录失败）
        boolean isWrong = verifyPassword("wrong123456", hashedPassword);
        System.out.println("错误密码验证结果：" + isWrong); // 输出false
    }
}