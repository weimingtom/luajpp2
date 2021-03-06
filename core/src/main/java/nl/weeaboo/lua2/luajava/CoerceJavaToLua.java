package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.util.HashMap;
import java.util.Map;

import nl.weeaboo.lua2.vm.LuaBoolean;
import nl.weeaboo.lua2.vm.LuaDouble;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

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
        COERCIONS.put(Boolean.TYPE, boolCoercion);
		COERCIONS.put(Byte.class, intCoercion);
        COERCIONS.put(Byte.TYPE, intCoercion);
        COERCIONS.put(Character.class, charCoercion);
        COERCIONS.put(Character.TYPE, charCoercion);
        COERCIONS.put(Short.class, intCoercion);
        COERCIONS.put(Short.TYPE, intCoercion);
		COERCIONS.put(Integer.class, intCoercion);
        COERCIONS.put(Integer.TYPE, intCoercion);
        COERCIONS.put(Long.class, longCoercion);
        COERCIONS.put(Long.TYPE, longCoercion);
        COERCIONS.put(Float.class, doubleCoercion);
        COERCIONS.put(Float.TYPE, doubleCoercion);
        COERCIONS.put(Double.class, doubleCoercion);
        COERCIONS.put(Double.TYPE, doubleCoercion);
		COERCIONS.put(String.class, stringCoercion);
	}

    /** Converts a sequence of values to an equivalent LuaTable */
    public static <T> LuaTable toTable(Iterable<? extends T> values, Class<T> type) {
        LuaTable table = new LuaTable();
        int i = 1;
        for (T value : values) {
            table.rawset(i, coerce(value, type));
            i++;
        }
        return table;
    }

    public static Varargs coerceArgs(Object[] values) {
        LuaValue[] luaArgs = new LuaValue[values.length];
        for (int n = 0; n < luaArgs.length; n++) {
            luaArgs[n] = coerce(values[n]);
        }
        return LuaValue.varargsOf(luaArgs);
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
