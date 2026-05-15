package cn.pzhdv.blog.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 基于 Jackson 的 JSON 工具类
 */
@Slf4j
public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    static {
        // 1. 注册 JavaTimeModule 以支持 JDK 8 的时间类型 (LocalDateTime等)
        MAPPER.registerModule(new JavaTimeModule());

        // 2. 取消将日期序列化为时间戳
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // 3. 设置全局日期格式
        MAPPER.setDateFormat(new SimpleDateFormat(STANDARD_FORMAT));

        // 4. 在序列化时，忽略值为 null 的字段 (可选)
         MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 5. 忽略在 JSON 中存在但 Java 对象中不存在的属性，防止崩溃
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 6. 忽略空 Bean 转 JSON 的错误
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * 将对象转换成json字符串
     */
    public static String objectToJson(Object data) {
        if (data == null) return null;
        try {
            return MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Object to JSON error", e);
        }
        return null;
    }

    /**
     * 将json结果集转化为对象
     */
    public static <T> T jsonToPojo(String jsonData, Class<T> beanType) {
        if (jsonData == null || jsonData.isEmpty()) return null;
        try {
            return MAPPER.readValue(jsonData, beanType);
        } catch (Exception e) {
            log.error("JSON to Pojo error", e);
        }
        return null;
    }

    /**
     * 将json数据转换成pojo对象list
     */
    public static <T> List<T> jsonToList(String jsonData, Class<T> beanType) {
        if (jsonData == null || jsonData.isEmpty()) return null;
        JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, beanType);
        try {
            return MAPPER.readValue(jsonData, javaType);
        } catch (Exception e) {
            log.error("JSON to List error", e);
        }
        return null;
    }
}