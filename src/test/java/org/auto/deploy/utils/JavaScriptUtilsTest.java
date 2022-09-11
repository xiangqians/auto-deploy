package org.auto.deploy.utils;

import lombok.extern.slf4j.Slf4j;
import org.auto.deploy.util.JavaScriptUtils;
import org.junit.Test;

import javax.script.ScriptEngine;
import java.util.List;
import java.util.Map;

/**
 * @author xiangqian
 * @date 12:33 2022/08/13
 */
@Slf4j
public class JavaScriptUtilsTest {

    @Test
    public void test() throws Exception {
        ScriptEngine engine = JavaScriptUtils.getScriptEngine();

//        System.out.println("-->" + JavaScriptUtils.getCompilable());
//        System.out.println(engine.getClass().getName());
//        try {
//            System.out.println("Result:" + engine.eval("function f() { return 1; }; f() + 1;"));
//        } catch (javax.script.ScriptException e) {
//            e.printStackTrace();
//        }


        String script = "print(\"Hello\", \"World!\");";
        script = "function test(){var array = customParam\n" +
                "        var result = [];\n" +
                "        for (var i = 0, length = array.length; i < length; i++) {\n" +
                "            result.push('[' + array[i] + ']');\n" +
                "        }\n" +
                "return {\n" +
                "    \"l1\": {\n" +
                "        \"l1_1\": [\n" +
                "            \"l1_1_1\",\n" +
                "            \"l1_1_2\"\n" +
                "        ],\n" +
                "        \"l1_2\": {\n" +
                "            \"l1_2_1\": 121\n" +
                "        }\n" +
                "    },\n" +
                "    \"l2\": {\n" +
                "        \"l2_1\": null,\n" +
                "        \"l2_2\": true,\n" +
                "        \"l2_3\": {}\n" +
                "    }\n" +
                "};};" +
                "test()";

        Object obj = JavaScriptUtils.execute(script, Map.of("customParam", List.of("name", 22, 33)), Map.class);
        log.debug("obj: {}", obj.getClass());
        log.debug("obj: {}", obj);

        log.debug("obj: {}", obj);
    }


}
