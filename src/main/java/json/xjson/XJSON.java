package json.xjson;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class XJSON {
	
	public static LinkedHashMap<String, Object> parse(String json) throws XJSONException {
		if(null == json || json.trim().isEmpty()) {
			throw new XJSONException("input json string can not be null or empty.");
		}
		return JSONDecoder.parseToMap(json);
	}

	public static LinkedList<Object> parseList(String json) throws XJSONException {
		if(null == json || json.trim().isEmpty()) {
			throw new XJSONException("input json string can not be null or empty.");
		}
		return JSONDecoder.parseToList(json);
	}
	
	public static <T> T parse(String json, Class<T> clazz) throws XJSONException {
		if(null == json || json.trim().isEmpty()) {
			throw new XJSONException("input json string can not be null or empty.");
		}
		return JSONDecoder.parseToClass(json,  clazz);
	}

	public static <T> LinkedList<T> parseList(String json, Class<T> clazz) throws XJSONException {
		if(null == json || json.trim().isEmpty()) {
			throw new XJSONException("input json string can not be null or empty.");
		}
		return JSONDecoder.parseToClassList(json, clazz);
	}
	
	public static String stringify(Object data) throws XJSONException  {
		return JSONEncoder.stringifyOneObject(data);
	}
	
	public static void setSerializer(Class<?> cls, XJSONSerializer serializer) {
		JSONEncoder.CODER_MAP.put(cls,  serializer);
	}
	
	public static void setDeserializer(Class<?> cls, XJSONDeserializer deserializer) {
		JSONReflect.CODER_MAP.put(cls,  deserializer);
	}
	
	public static void useStrictJsonMode(boolean mode) {
		JSONReflect.strictJsonMode = mode;
	}
	
	public static void useStrictClassMode(boolean mode) {
		JSONReflect.strictClassMode = mode;
	}

	public static void useBeautifyMode(boolean mode) {
		JSONEncoder.BEAUTIFY = mode;
	}
}
