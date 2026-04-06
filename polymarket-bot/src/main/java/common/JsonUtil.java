package common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

public class JsonUtil {

    public static String toJson(Object o) {
        return JSON.toJSONString(o);
    }

    public static <T> T toObj(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    public static <T> T toObj(String json, TypeReference<T> type) {
        return JSON.parseObject(json, type);
    }

    public static JSONObject toJSONObj(String data) {
        return JSONObject.parseObject(data);
    }
}
