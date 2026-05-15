package cn.pzhdv.blog.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * IP地址工具类
 * 本地/内网/生产环境都能正常获取IP
 */
public class IpUtils {

    private static final List<String> PROXY_HEADERS = Arrays.asList(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    );

    public static String getClientIpForLog(HttpServletRequest request) {
        try {
            return getClientIpAddress(request);
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        for (String header : PROXY_HEADERS) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                if ("X-Forwarded-For".equalsIgnoreCase(header) || "HTTP_X_FORWARDED_FOR".equalsIgnoreCase(header)) {
                    return ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return "127.0.0.1";
        }

        return isValidIp(remoteAddr) ? remoteAddr : "unknown";
    }

    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        String trimmed = ip.trim();
        return !"unknown".equalsIgnoreCase(trimmed);
    }
}