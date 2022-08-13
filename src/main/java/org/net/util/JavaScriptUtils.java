package org.net.util;

import org.apache.commons.collections4.MapUtils;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * nashorn - JavaScript引擎
 *
 * @author xiangqian
 * @date 12:36 2022/08/13
 */
public class JavaScriptUtils {

    public static <T> T execute(String script, Map<String, Object> params, Class<T> rtnType) throws ScriptException {
        ScriptEngine engine = getScriptEngine();
        CompiledScript compiledScript = ((Compilable) engine).compile(script);

        // 绑定参数
        Bindings bindings = null;
        if (MapUtils.isNotEmpty(params)) {
            bindings = engine.createBindings();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                bindings.put(param.getKey(), param.getValue());
            }
        }
        return getRtnType(compiledScript.eval(bindings), rtnType);
    }


    public static <T> T getRtnType(Object rtnValue, Class<T> rtnType) throws ClassCastException {
        if (Objects.isNull(rtnValue)) {
            return null;
        }

        // object
        if (rtnType == Object.class) {
            return (T) rtnValue;
        }

        // boolean
        if (rtnType == Boolean.class) {
            if (rtnValue instanceof Boolean) {
                return (T) rtnValue;
            }
            throw new ClassCastException();
        }

        // integer
        if (rtnType == Integer.class) {
            if (rtnValue instanceof Integer) {
                return (T) rtnValue;
            }
            throw new ClassCastException();
        }

        // string
        if (rtnType == String.class) {
            if (rtnValue instanceof String) {
                return (T) rtnValue;
            }
            throw new ClassCastException();
        }

        // list
        if (rtnType == List.class) {
            if (rtnValue instanceof ScriptObjectMirror) {
                ScriptObjectMirror jsObject = (ScriptObjectMirror) rtnValue;
                return (T) Optional.ofNullable(jsObjToJavaObj(jsObject))
                        .map(map -> map.values().stream().collect(Collectors.toList()))
                        .orElse(null);
            }
            throw new ClassCastException();
        }

        // map
        if (rtnType == Map.class) {
            if (rtnValue instanceof ScriptObjectMirror) {
                ScriptObjectMirror jsObject = (ScriptObjectMirror) rtnValue;
                return (T) jsObjToJavaObj(jsObject);
            }
            throw new ClassCastException();
        }

        throw new UnsupportedOperationException();
    }

    public static Map<String, Object> jsObjToJavaObj(ScriptObjectMirror jsObj) {
        if (Objects.isNull(jsObj)) {
            return null;
        }

        if (MapUtils.isEmpty(jsObj)) {
            return Collections.emptyMap();
        }

        Map<String, Object> map = new HashMap<>(jsObj.size());
        for (Map.Entry<String, Object> entry : jsObj.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof ScriptObjectMirror) {
                value = jsObjToJavaObj((ScriptObjectMirror) value);
            }
            map.put(entry.getKey(), value);
        }
        return map;
    }

    public static ScriptEngine getScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        // OR
        // ScriptEngine engine = manager.getEngineByExtension("js");
        return engine;
    }

}