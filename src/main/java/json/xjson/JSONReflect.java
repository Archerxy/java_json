package json.xjson;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

class JSONReflect {

	static final String BOOL_TYPE = "boolean";
	static final String BYTE_TYPE = "byte";
	static final String CHAR_TYPE = "char";
	static final String SHORT_TYPE = "short";
	static final String INT_TYPE = "int";
	static final String LONG_TYPE = "long";
	static final String FLOAT_TYPE = "float";
	static final String DOUBLE_TYPE = "double";
	
	static final ConcurrentHashMap<Class<?>, XJSONDeserializer> CODER_MAP = 
			new ConcurrentHashMap<>();

	static final DateFormat DEFAULT_DATE_FORMAT = 
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static final DateTimeFormatter DEFAULT_TIME_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	

	static boolean strictJsonMode = false;
	static boolean strictClassMode = false;
	
	static final String INNER_CLASS_FIELD = "this$";
	
	static {
		/**
		 * popular class serializer
		 * */
		CODER_MAP.put(Boolean.class, (String obj) -> {return Boolean.parseBoolean(obj);});
		CODER_MAP.put(Byte.class, (String obj) -> {return Byte.parseByte(obj);});
		CODER_MAP.put(Character.class, (String obj) -> {
			if(obj.length() != 1) {
				throw new XJSONException("unknown Charater value " + obj);
			}
			return obj.charAt(0);
		});
		CODER_MAP.put(Short.class, (String obj) -> {return Short.parseShort(obj);});
		CODER_MAP.put(Integer.class, (String obj) -> {return Integer.parseInt(obj);});
		CODER_MAP.put(Long.class, (String obj) -> {return Long.parseLong(obj);});
		CODER_MAP.put(Float.class, (String obj) -> {return Float.parseFloat(obj);});
		CODER_MAP.put(Double.class, (String obj) -> {return Double.parseDouble(obj);});
	}
	
	@SuppressWarnings("unchecked")
	static <T> LinkedList<T> reflectOneList(LinkedList<Object> obj, Class<T> clazz) 
			throws XJSONException {
		LinkedList<T> ret = new LinkedList<T>();
		for(Object val: obj) {
			ret.add((T) fromJavaType(clazz, val));
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	static <T> T reflectOneClass(LinkedHashMap<String, Object> obj, Class<T> clazz) 
			throws XJSONException {
		return (T) fromJavaType(clazz, obj);
	}
	
	
	static void reflectToField(Field f, Object classObj, Object jsonData) 
			throws Exception {
		Type type = f.getGenericType();
		Object data = fromJavaType(type, jsonData);
		if(data == null) {
			throw new XJSONException("unknown field type '" + 
					f.getName() + "'(" + f.getType().getName()+")");
		}
		f.setAccessible(true);
		f.set(classObj, data);
	}
	
	@SuppressWarnings("unchecked")
	static Object fromJavaType(Type javaType, Object val) 
			throws XJSONException {
		if(javaType.getClass().equals(Class.class)) {
			Class<?> cls = (Class<?>) javaType;
			if(CODER_MAP.contains(cls)) {
				String data = null;
				if(val instanceof String) {
					data = (String) val;
				}
				if(val instanceof Byte) {
					data = String.valueOf((byte) val);
				}
				if(val instanceof Character) {
					data = String.valueOf((char) val);
				}
				if(val instanceof Integer) {
					data = String.valueOf((int) val);
				}
				if(val instanceof Long) {
					data = String.valueOf((long) val);
				}
				if(val instanceof Double) {
					data = String.valueOf((double) val);
				}
				if(data == null) {
					throw new XJSONException("unknown value type " + val.getClass().getName());
				}
				return CODER_MAP.get(cls).deserialize(data);
			}
			if(cls.isPrimitive()) {
				return reflectToPrimitive(cls, val);
			} else {
				return reflectToPopularClass(cls, val);
			}
		} else if(javaType instanceof ParameterizedType) {
			Class<?> cls = (Class<?>)(((ParameterizedType)javaType).getRawType());
			if(Collection.class.isAssignableFrom(cls)) {
				return reflectCollection(cls, (ParameterizedType)javaType, 
						(LinkedList<Object>) val);
			}
			if(Map.class.isAssignableFrom(cls)) {
				return reflectMap(cls, (ParameterizedType)javaType, 
						(LinkedHashMap<String, Object>) val);
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	static Collection<Object> reflectCollection(Class<?> cls, 
			ParameterizedType javaType, LinkedList<Object> val) 
			throws XJSONException {
		Collection<Object> instance;
		if(List.class.isAssignableFrom(cls)) {
			if(cls.isInterface()) {
				instance = new LinkedList<>();
			} else {
				try {
					instance = (Collection<Object>) cls.newInstance();
				} catch(Exception ignore) {
					throw new XJSONException("can not construct class '" +
							cls.getName() + "'");
				}
			}
		} else if(Set.class.isAssignableFrom(cls)) {
			if(cls.isInterface()) {
				instance = new LinkedHashSet<>();
			} else {
				try {
					instance = (Collection<Object>) cls.newInstance();
				} catch(Exception ignore) {
					throw new XJSONException("can not construct class '" +
							cls.getName() + "'");
				}
			}
		} else if(Queue.class.isAssignableFrom(cls)) {
			if(cls.isInterface()) {
				instance = new ArrayBlockingQueue<>(val.size());
			} else {
				try {
					instance = (Collection<Object>) cls.newInstance();
				} catch(Exception ignore) {
					throw new XJSONException("can not construct class '" +
							cls.getName() + "'");
				}
			}
		} else {
			throw new XJSONException("unknown collection like type '" +
					cls.getName() + "'");
		}
		Type[] genericTypes = javaType.getActualTypeArguments();
		if(genericTypes == null || genericTypes.length != 1) {
			throw new XJSONException("invalid collection like type '" + 
						javaType.getTypeName() + "'");
		}
		for(Object o: val) {
			instance.add(fromJavaType(genericTypes[0], o));
		}
		return instance;
	}
	

	@SuppressWarnings("unchecked")
	static Map<Object, Object> reflectMap(Class<?> cls, 
			ParameterizedType javaType, LinkedHashMap<String, Object> val) 
			throws XJSONException {
		Map<Object, Object> instance;
		if(cls.isInterface()) {
			instance = new LinkedHashMap<>();
		} else {
			try {
				instance = (Map<Object, Object>) cls.newInstance();
			} catch(Exception ignore) {
				throw new XJSONException("can not construct class '" +
						cls.getName() + "'");
			}
		}
		Type[] genericTypes = ((ParameterizedType) javaType).getActualTypeArguments();
		if(genericTypes == null || genericTypes.length != 2) {
			throw new XJSONException("invalid collection like type '" + 
						javaType.getTypeName() + "'");
		}
		for(Map.Entry<String, Object> entry: val.entrySet()) {
			instance.put(fromJavaType(genericTypes[0], entry.getKey()),
					fromJavaType(genericTypes[1], entry.getValue()));
		}
		
		return instance;
	}
	
	static Object reflectToPrimitive(Class<?> cls,  Object val) 
			throws XJSONException {
		if(BOOL_TYPE.equals(cls.getName())) {
			return (boolean) val;
		} else if(BYTE_TYPE.equals(cls.getName())) {
			
			return (byte)((int) val);
		} else if(CHAR_TYPE.equals(cls.getName())) {
			return (char) val;
		} else if(SHORT_TYPE.equals(cls.getName())) {
			return (short) val;
		} else if(INT_TYPE.equals(cls.getName())) {
			return (int) val;
		} else if(LONG_TYPE.equals(cls.getName())) {
			return (long) val;
		} else if(FLOAT_TYPE.equals(cls.getName())) {
			if(val instanceof Double) {
				return ((Double) val).floatValue();
			}
			return (float) val;
		} else if(DOUBLE_TYPE.equals(cls.getName())) {
			return (double) val;
		}
		throw new XJSONException("unknown primitive type '" 
				+ cls.getName() + "'");
	}

	@SuppressWarnings("unchecked")
	static Object reflectToPopularClass(Class<?> cls,  Object val) 
			throws XJSONException {
		if(Date.class.isAssignableFrom(cls)) {
			try {
				return DEFAULT_DATE_FORMAT.parse((String)val);
			} catch(Exception ignore) {
				throw new XJSONException("can not parse '"+
						((String) val)+"' to Date");
			}
		} else if(cls.equals(LocalDate.class)) {
			try {
				return LocalDate.parse((String)val, DEFAULT_TIME_FORMAT);
			} catch(Exception ignore) {
				throw new XJSONException("can not parse '"+
						((String) val)+"' to LocalDate");
			}
		} else if(cls.equals(LocalTime.class)) {
			try {
				return LocalTime.parse((String)val, DEFAULT_TIME_FORMAT);
			} catch(Exception ignore) {
				throw new XJSONException("can not parse '"+
						((String) val)+"' to LocalTime");
			}
		} else if(cls.equals(LocalDateTime.class)) {
			try {
				return LocalDateTime.parse((String)val, DEFAULT_TIME_FORMAT);
			} catch(Exception ignore) {
				throw new XJSONException("can not parse '"+
						((String) val)+"' to LocalDateTime");
			}
		} else if(cls.equals(BigInteger.class)) {
			try {
				return BigInteger.valueOf((long) val);
			} catch(Exception ignore) {
				throw new XJSONException("can not parse '"+
						((String) val)+"' to BigInteger");
			}
		} else if(cls.equals(BigDecimal.class)) {
			try {
				return BigDecimal.valueOf((double) val);
			} catch(Exception ignore) {
				throw new XJSONException("can not parse '"+
						((String) val)+"' to BigDecimal");
			}
		}
		if(val instanceof LinkedHashMap) {
			return reflectUnknownClass(cls, (LinkedHashMap<String, Object>) val);
		}
		return val;
	}
	
	static Object reflectUnknownClass(Class<?> cls, LinkedHashMap<String, Object> val) 
			throws XJSONException {
		Object instance;
		try {
			instance = cls.newInstance();
		} catch (Exception e) {
			throw new XJSONException(
					"no arguments constructor is required with class '" 
					+ cls.getName() + "'"); 
		}
		Field[] fields = cls.getDeclaredFields();
		TreeSet<String> fieldNameSet = null;
		if(strictJsonMode) {
			fieldNameSet = new TreeSet<>();
		}
		
		for(Field f: fields) {
			if(f.getName().startsWith(INNER_CLASS_FIELD)) {
				continue;
			}
			if(Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			if(Modifier.isTransient(f.getModifiers())) {
				continue;
			}
			Object fieldVal = val.getOrDefault(f.getName(), null);
			if(fieldVal != null) {
				try {
					f.setAccessible(true);
					f.set(instance, fromJavaType(f.getGenericType(), fieldVal));
				} catch(Exception e) {
					throw new XJSONException(
							XJSONException.getErrorMsg(f, val.get(f.getName())));
				}
			} else {
				if(strictClassMode) {
					throw new XJSONException(
							XJSONException.getErrorMsg(f.getName(), cls, true));
				}
			}
			if(strictJsonMode) {
				fieldNameSet.add(f.getName());
			}
		}
		if(strictJsonMode) {
			for(String k: val.keySet()) {
				if(!fieldNameSet.contains(k)) {
					throw new XJSONException(
							XJSONException.getErrorMsg(k, cls, false));
				}
			}
		}
		return instance;
	}
}
