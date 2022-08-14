package org.net.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author xiangqian
 * @date 14:06 2022/08/14
 */
public class JacksonUtils {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        // Include
//        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS); // 默认
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL); // 属性为NULL不序列化

//        OBJECT_MAPPER.findAndRegisterModules();

        // JavaTimeModule
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateFormat)));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateFormat)));
        OBJECT_MAPPER.registerModule(javaTimeModule);
    }

    // ================ 序列化 ================

    public static String toJson(Object obj) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    public static String toFormatJson(Object obj) throws IOException {
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    public static byte[] toBytes(Object obj) throws IOException {
        return OBJECT_MAPPER.writeValueAsBytes(obj);
    }

    // ================ 反序列化 ================

    public static <T> T toObject(String json, Class<T> tClass) throws IOException {
        return OBJECT_MAPPER.readValue(json, tClass);
    }

    public static <T> T toObject(String json, TypeReference<T> typeReference) throws IOException {
        return OBJECT_MAPPER.readValue(json, typeReference);
    }

    public static <T> T toObject(byte[] bytes, Class<T> tClass) throws IOException {
        return OBJECT_MAPPER.readValue(bytes, tClass);
    }

    public static <T> T toObject(byte[] bytes, TypeReference<T> typeReference) throws IOException {
        return OBJECT_MAPPER.readValue(bytes, typeReference);
    }

    public static JsonNode toJsonNode(String json) throws IOException {
        return OBJECT_MAPPER.readTree(json);
    }

    public static <T> T toObject(JsonNode jsonNode, Class<T> tClass) throws IOException {
        return toObject(toJson(jsonNode), tClass);
    }

    public static <T> T toObject(JsonNode jsonNode, TypeReference<T> typeReference) throws IOException {
        return toObject(toJson(jsonNode), typeReference);
    }

    // ================ create ================

    public static ObjectNode createObjectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return OBJECT_MAPPER.createArrayNode();
    }

}
