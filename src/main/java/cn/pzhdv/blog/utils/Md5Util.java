package cn.pzhdv.blog.utils;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MD5 工具类
 * <p>
 * 提供 MD5 哈希计算功能，用于生成缓存 key 等场景。
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-07
 */
public class Md5Util {

    private Md5Util() {
    }

    /**
     * 计算字符串的 MD5 哈希值
     *
     * @param input 输入字符串
     * @return MD5 哈希值（32位十六进制），计算失败时返回原字符串
     */
    public static String md5(String input) {
        if (input == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return input;
        }
    }

    /**
     * 将多个参数拼接后计算 MD5 哈希值
     * <p>
     * 使用 "|" 作为分隔符拼接参数，null 值会被替换为空字符串。
     * </p>
     *
     * @param params 参数列表
     * @return MD5 哈希值
     */
    public static String md5Of(Object... params) {
        if (params == null || params.length == 0) {
            return "";
        }
        String raw = Stream.of(params)
                .map(p -> p != null ? p.toString() : "")
                .collect(Collectors.joining("|"));
        return md5(raw);
    }
}
