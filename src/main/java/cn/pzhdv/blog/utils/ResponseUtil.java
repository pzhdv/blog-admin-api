package cn.pzhdv.blog.utils;

import cn.pzhdv.blog.result.Result;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author PanZonghui
 * @since 2025-06-25 13:10:16
 */
@Slf4j
public class ResponseUtil {
    public static void out(HttpServletResponse response, Result result) {
        try {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json; charset=utf-8");
            String json = JsonUtils.objectToJson(result);

            try (ServletOutputStream outputStream = response.getOutputStream()) {
                outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        } catch (IOException e) {
            log.error("响应输出异常", e);
        }
    }
}