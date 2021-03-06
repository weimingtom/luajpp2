package nl.weeaboo.lua2.link;

import java.lang.reflect.Array;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.LuaUtil;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public class LuaFunctionLink extends LuaLink {

    private static final long serialVersionUID = 1L;

	private LuaClosure func;
	private String funcName;
	private Object args;

    public LuaFunctionLink(LuaRunState lrs, String funcName, Object... args) {
        this(lrs, lrs.getGlobalEnvironment(), funcName, args);
    }
    public LuaFunctionLink(LuaRunState lrs, LuaValue environment, String funcName, Object... args) {
        super(lrs, environment);

		this.funcName = funcName;
		this.args = args;
	}

    public LuaFunctionLink(LuaRunState lrs, LuaClosure func, Varargs args) {
        this(lrs, lrs.getGlobalEnvironment(), func, args);
    }
    public LuaFunctionLink(LuaRunState lrs, LuaValue environment, LuaClosure func, Varargs args) {
        super(lrs, environment);

		this.func = func;
		this.args = LuaUtil.copyArgs(args);
	}

	@Override
	protected void init() throws LuaException {
		if (func != null) {
			pushCall(func, (Varargs)args);
		} else if (funcName != null) {
			pushCall(funcName, (Object[])args);
		} else {
			throw new LuaException("Attempting to call a nil value");
		}
	}

	protected void printArgs(StringBuilder out, Object args) {
		if (args == null) {
			return;
		}

		boolean isArray = args.getClass().isArray();
		if (isArray) {
			int length = Array.getLength(args);
			for (int n = 0; n < length; n++) {
				out.append(", ").append(Array.get(args, n));
			}
		} else if (args instanceof Varargs) {
			Varargs vargs = (Varargs)args;
			for (int n = 1; n <= vargs.narg(); n++) {
				out.append(", ").append(vargs.arg(n));
			}
		} else {
			out.append(", ").append(args);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(128);
		sb.append(getClass().getSimpleName());
		sb.append('(');
		sb.append(funcName != null ? funcName : String.format("closure{%s}", func.getPrototype().toString()));
		printArgs(sb, getImplicitArgs());
		printArgs(sb, args);
		sb.append(')');
		return sb.toString();
	}

}
