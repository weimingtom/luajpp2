package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaValue.valueOf;

import java.io.ObjectStreamException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.weeaboo.lua2.io.IReadResolveSerializable;
import nl.weeaboo.lua2.io.IWriteReplaceSerializable;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class ClassInfo implements IWriteReplaceSerializable {

	private static final Comparator<Method> methodSorter = new MethodSorter();

	private final Class<?> clazz;
	private final boolean isArray;

	private transient ClassMetaTable metaTable;

	private transient ConstructorInfo[] constrs;
	private transient Map<LuaString, Field> fields;
	private transient Map<LuaString, MethodInfo[]> methods;

	public ClassInfo(Class<?> c) {
		clazz = c;
		isArray = c.isArray();
	}

    @Override
    public Object writeReplace() throws ObjectStreamException {
		return new ClassInfoRef(clazz);
	}

    public Object newInstance(Varargs luaArgs) throws IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

		ConstructorInfo constr = findConstructor(luaArgs);
		if (constr == null) {
            throw new LuaError("No suitable constructor found for: " + clazz.getName());
		}

        Class<?>[] paramTypes = constr.getParams();
        Object[] javaArgs = new Object[paramTypes.length];
        CoerceLuaToJava.coerceArgs(javaArgs, luaArgs, paramTypes);
		return constr.getConstructor().newInstance(javaArgs);
	}

	@Override
	public int hashCode() {
		return clazz.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClassInfo) {
			ClassInfo ci = (ClassInfo)obj;
			return clazz.equals(ci.clazz);
		}
		return false;
	}

	public Class<?> getWrappedClass() {
		return clazz;
	}

	public boolean isArray() {
		return isArray;
	}

	protected ConstructorInfo findConstructor(Varargs luaArgs) {
		ConstructorInfo bestMatch = null;
		int bestScore = Integer.MAX_VALUE;

        for (ConstructorInfo constr : getConstructors()) {
            int score = CoerceLuaToJava.scoreParamTypes(luaArgs, constr.getParams());
			if (score == 0) {
                return constr; // Perfect match, return at once
			} else if (score < bestScore) {
				bestScore = score;
                bestMatch = constr;
			}
		}

		return bestMatch;
	}

	public ConstructorInfo[] getConstructors() {
		if (constrs == null) {
			Constructor<?> cs[] = clazz.getConstructors();

			constrs = new ConstructorInfo[cs.length];
			for (int n = 0; n < cs.length; n++) {
				constrs[n] = new ConstructorInfo(n, cs[n]);
			}
		}
		return constrs;
	}

	public ClassMetaTable getMetatable() {
		if (metaTable == null) {
			metaTable = new ClassMetaTable(this);
		}
		return metaTable;
	}

	public Field getField(LuaValue name) {
		if (fields == null) {
			fields = new HashMap<LuaString, Field>();
			for (Field f : clazz.getFields()) {
				fields.put(valueOf(f.getName()), f);
			}
		}
		return fields.get(name);
	}

	public MethodInfo[] getMethods(LuaValue name) {
		if (methods == null) {
			Method marr[] = clazz.getMethods();
			Arrays.sort(marr, methodSorter);

			methods = new HashMap<LuaString, MethodInfo[]>();

			String curName = null;
			List<MethodInfo> list = new ArrayList<MethodInfo>();
			for (Method m : marr) {
                // Workaround for https://bugs.openjdk.java.net/browse/JDK-4283544
                m.setAccessible(true);

				if (!m.getName().equals(curName)) {
					if (curName != null) {
						methods.put(valueOf(curName), list.toArray(new MethodInfo[list.size()]));
					}
					curName = m.getName();
					list.clear();
				}
				list.add(new MethodInfo(m));
			}

			if (curName != null) {
				methods.put(LuaString.valueOf(curName), list.toArray(new MethodInfo[list.size()]));
			}
		}
		return methods.get(name);
	}

	public boolean hasMethod(LuaString name) {
		return getMethods(name) != null;
	}

	@LuaSerializable
    private static class ClassInfoRef implements IReadResolveSerializable {

		private static final long serialVersionUID = 1L;

		private final Class<?> clazz;

		public ClassInfoRef(Class<?> clazz) {
			this.clazz = clazz;
		}

		@Override
        public Object readResolve() throws ObjectStreamException {
			return LuajavaLib.getClassInfo(clazz);
		}
	}

	private static class MethodSorter implements Comparator<Method> {

        @Override
        public int compare(Method m1, Method m2) {
            return m1.getName().compareTo(m2.getName());
        }
    }

}
