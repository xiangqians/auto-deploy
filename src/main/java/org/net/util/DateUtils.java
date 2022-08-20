package org.net.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author xiangqian
 * @date 23:40 2022/08/20
 */
public class DateUtils {

    public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);

    public static LocalDateTime parsForLocalDateTime(String text) {
        return parsForLocalDateTime(text, DEFAULT_FORMATTER);
    }

    public static LocalDateTime parsForLocalDateTime(String text, String pattern) {
        return parsForLocalDateTime(text, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime parsForLocalDateTime(String text, DateTimeFormatter dateTimeFormatter) {
        return LocalDateTime.parse(text, dateTimeFormatter);
    }

    public static String format(LocalDateTime time) {
        return format(time, DEFAULT_FORMATTER);
    }

    public static String format(LocalDateTime time, String pattern) {
        return format(time, DateTimeFormatter.ofPattern(pattern));
    }

    public static String format(LocalDateTime time, DateTimeFormatter dateTimeFormatter) {
        return dateTimeFormatter.format(time);
    }

    public static LocalDateTime timestampToLocalDateTime(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static long dateToTimestamp(LocalDateTime time) {
        ZoneId zone = ZoneId.systemDefault();
        return time.atZone(zone).toInstant().toEpochMilli();
    }

}
