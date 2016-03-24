package nl.weeaboo.lua2.lib;

import static org.luaj.vm2.LuaValue.NIL;

import java.util.HashMap;
import java.util.Map;

import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

public class CoerceJavaToLua {

	public static interface Coercion {
		public LuaValue coerce(Object javaValue);
	}

	private static Map<Class<?>, Coercion> COERCIONS = new HashMap<Class<?>, Coercion>();

	static {
		Coercion boolCoercion = new Coercion() {
            @Override
            public LuaValue coerce(Object javaValue) {
				boolean b = ((Boolean)javaValue).booleanValue();
				return (b ? LuaBoolean.TRUE : LuaBoolean.FALSE);
			}
		};
		Coercion charCoercion = new Coercion() {
            @Override
			public LuaValue coerce(Object javaValue) {
				char c = ((Character)javaValue).charValue();
				return LuaInteger.valueOf(c);
			}
		};
		Coercion intCoercion = new Coercion() {
            @Override
            public LuaValue coerce(Object javaValue) {
				int i = ((Number)javaValue).intValue();
				return LuaInteger.valueOf(i);
			}
		};
		Coercion longCoercion = new Coercion() {
            @Override
			public LuaValue coerce(Object javaValue) {
				long i = ((Number)javaValue).longValue();
				return LuaDouble.valueOf(i);
			}
		};
		Coercion doubleCoercion = new Coercion() {
            @Override
			public LuaValue coerce(Object javaValue) {
				double d = ((Number)javaValue).doubleValue();
				return LuaDouble.valueOf(d);
			}
		};
		Coercion stringCoercion = new Coercion() {
            @Override
			public LuaValue coerce(Object javaValue) {
				return LuaString.valueOf(javaValue.toString());
			}
		};

		COERCIONS.put(Boolean.class, boolCoercion);
		COERCIONS.put(Byte.class, intCoercion);
		COERCIONS.put(Character.class, charCoercion);
		COERCIONS.put(Short.class, intCoercion);
		COERCIONS.put(Integer.class, intCoercion);
		COERCIONS.put(Long.class, longCoercion);
		COERCIONS.put(Float.class, doubleCoercion);
		COERCIONS.put(Double.class, doubleCoercion);
		COERCIONS.put(String.class, stringCoercion);
	}

    public static LuaValue coerce(Object obj) {
        if (obj == null) {
            return NIL;
        }
        return coerce(obj, obj.getClass());
    }

    public static LuaValue coerce(Object obj, Class<?> declaredType) {
        if (obj == null) {
			return NIL;
		}

        Coercion c = COERCIONS.get(declaredType);
		if (c != null) {
            // A specialized coercion was found, use it
            return c.coerce(obj);
		}

        if (LuaValue.class.isAssignableFrom(declaredType)) {
            // Java object is a Lua type
            return (LuaValue)obj;
		}

        // Use the general Java Object -> Lua conversion
        return LuajavaLib.toUserdata(obj, declaredType);
	}

}
