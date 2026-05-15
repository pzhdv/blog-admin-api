package cn.pzhdv.blog.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 获取某一天的开始时间（00:00:00）
     *
     * @param date 输入的日期
     * @return 当天的开始时间
     */
    public static Date startOfDay(Date date) {
        return toUtilDate(LocalDateTime.of(toLocalDate(date), java.time.LocalTime.MIN));
    }

    /**
     * 获取某一天的结束时间（23:59:59.999）
     *
     * @param date 输入的日期
     * @return 当天的结束时间
     */
    public static Date endOfDay(Date date) {
        return toUtilDate(LocalDateTime.of(toLocalDate(date), java.time.LocalTime.MAX));
    }

    /**
     * 将 java.util.Date 转换为 java.time.LocalDate
     *
     * @param date java.util.Date 对象
     * @return java.time.LocalDate 对象
     */
    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 将 java.time.LocalDateTime 转换为 java.util.Date
     *
     * @param dateTime java.time.LocalDateTime 对象
     * @return java.util.Date 对象
     */
    private static Date toUtilDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 将日期字符串解析为 Date 对象
     *
     * @param dateString 日期字符串，格式为 "yyyy-MM-dd"
     * @return Date 对象
     */
    public static Date parseDate(String dateString) {
        LocalDate localDate = LocalDate.parse(dateString, DATE_FORMATTER);
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 将 Date 对象格式化为字符串
     *
     * @param date Date 对象
     * @return 格式化后的日期字符串，格式为 "yyyy-MM-dd"
     */
    public static String formatDate(Date date) {
        return DATE_FORMATTER.format(toLocalDate(date));
    }
}