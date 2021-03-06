/*******************************************************************************
 * Copyright (c) 2008 LuaJ. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package nl.weeaboo.lua2.scriptengine;

import static nl.weeaboo.lua2.vm.LuaConstants.INDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaConstants.TNIL;
import static nl.weeaboo.lua2.vm.LuaConstants.TNUMBER;
import static nl.weeaboo.lua2.vm.LuaConstants.TSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.TUSERDATA;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.compiler.LoadState;
import nl.weeaboo.lua2.luajava.CoerceJavaToLua;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaFunction;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Prototype;
import nl.weeaboo.lua2.vm.Varargs;

/**
 *
 * @author jim_roseborough
 */
public class LuaScriptEngine implements ScriptEngine, Compilable {

    private static final String __NAME__ = "Luajpp2";
    private static final String __SHORT_NAME__ = "Luajpp2";
	private static final String __LANGUAGE__ = "lua";
	private static final String __LANGUAGE_VERSION__ = "5.1";
	private static final String __ARGV__ = "arg";
	private static final String __FILENAME__ = "?";

	private static final ScriptEngineFactory myFactory = new LuaScriptEngineFactory();

	private ScriptContext defaultContext;

	private final LuaValue _G;

    public LuaScriptEngine(LuaRunState lrs) {
		// create globals
        _G = lrs.getGlobalEnvironment();

		// set up context
		ScriptContext ctx = new SimpleScriptContext();
		ctx.setBindings(createBindings(), ScriptContext.ENGINE_SCOPE);
		setContext(ctx);

		// set special values
		put(LANGUAGE_VERSION, __LANGUAGE_VERSION__);
		put(LANGUAGE, __LANGUAGE__);
		put(ENGINE, __NAME__);
        put(ENGINE_VERSION, LuaConstants.getEngineVersion());
		put(ARGV, __ARGV__);
		put(FILENAME, __FILENAME__);
		put(NAME, __SHORT_NAME__);
		put("THREADING", null);
	}

	@Override
	public Object eval(String script) throws ScriptException {
		return eval(new StringReader(script));
	}

	@Override
	public Object eval(String script, ScriptContext context) throws ScriptException {
		return eval(new StringReader(script), context);
	}

	@Override
	public Object eval(String script, Bindings bindings) throws ScriptException {
		return eval(new StringReader(script), bindings);
	}

	@Override
	public Object eval(Reader reader) throws ScriptException {
		return eval(reader, getContext());
	}

	@Override
	public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
		return compile(reader).eval(scriptContext);
	}

	@Override
	public Object eval(Reader reader, Bindings bindings) throws ScriptException {
		ScriptContext c = getContext();
		Bindings current = c.getBindings(ScriptContext.ENGINE_SCOPE);
		c.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
		Object result = eval(reader);
		c.setBindings(current, ScriptContext.ENGINE_SCOPE);
		return result;
	}

	@Override
	public void put(String key, Object value) {
		Bindings b = getBindings(ScriptContext.ENGINE_SCOPE);
		b.put(key, value);
	}

	@Override
	public Object get(String key) {
		Bindings b = getBindings(ScriptContext.ENGINE_SCOPE);
		return b.get(key);
	}

	@Override
	public Bindings getBindings(int scope) {
		return getContext().getBindings(scope);
	}

	@Override
	public void setBindings(Bindings bindings, int scope) {
		getContext().setBindings(bindings, scope);
	}

	@Override
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	@Override
	public ScriptContext getContext() {
		return defaultContext;
	}

	@Override
	public void setContext(ScriptContext context) {
		defaultContext = context;
	}

	@Override
	public ScriptEngineFactory getFactory() {
		return myFactory;
	}

	@Override
	public CompiledScript compile(String script) throws ScriptException {
		return compile(new StringReader(script));
	}

	@Override
	public CompiledScript compile(Reader reader) throws ScriptException {
		try {
			InputStream ris = new Utf8Encoder(reader);
			try {
				final LuaFunction f = LoadState.load(ris, "script", null);
				if (f.isclosure()) {
					// most compiled functions are closures with prototypes
					final Prototype p = f.checkclosure().getPrototype();
					return new CompiledScriptImpl() {
						@Override
						protected LuaFunction newFunctionInstance() {
							return new LuaClosure(p, null);
						}
					};
				} else {
					// when luajc is used, functions are java class instances
					final Class<?> c = f.getClass();
					return new CompiledScriptImpl() {
						@Override
						protected LuaFunction newFunctionInstance() throws ScriptException {
							try {
								return (LuaFunction) c.newInstance();
							} catch (Exception e) {
								throw new ScriptException("instantiation failed: " + e.toString());
							}
						}
					};
				}
			} catch (LuaError lee) {
				throw new ScriptException(lee.getMessage());
			} finally {
				ris.close();
			}
		} catch (Throwable t) {
			throw new ScriptException("eval threw " + t.toString());
		}
	}

	abstract protected class CompiledScriptImpl extends CompiledScript {
		abstract protected LuaFunction newFunctionInstance() throws ScriptException;

		@Override
		public ScriptEngine getEngine() {
			return LuaScriptEngine.this;
		}

		@Override
		public Object eval(ScriptContext context) throws ScriptException {
			Bindings b = context.getBindings(ScriptContext.ENGINE_SCOPE);
			LuaFunction f = newFunctionInstance();
			ClientBindings cb = new ClientBindings(b);
			f.setfenv(cb.env);
            Varargs result = f.invoke(NONE);
			cb.copyGlobalsToBindings();
			return result;
		}
	}

	public class ClientBindings {
		public final Bindings b;
		public final LuaTable env;

		public ClientBindings(Bindings b) {
			this.b = b;
			this.env = new LuaTable();
            env.setmetatable(LuaValue.tableOf(new LuaValue[] { INDEX, _G }));
			this.copyBindingsToGlobals();
		}

		public void copyBindingsToGlobals() {
			for (Iterator<String> i = b.keySet().iterator(); i.hasNext();) {
				String key = i.next();
				Object val = b.get(key);
				LuaValue luakey = toLua(key);
				LuaValue luaval = toLua(val);
				env.set(luakey, luaval);
				i.remove();
			}
		}

		private LuaValue toLua(Object javaValue) {
            if (javaValue == null) {
                return NIL;
            } else if (javaValue instanceof LuaValue) {
                return (LuaValue)javaValue;
            } else {
                return CoerceJavaToLua.coerce(javaValue);
            }
		}

		public void copyGlobalsToBindings() {
			LuaValue[] keys = env.keys();
			for (int i = 0; i < keys.length; i++) {
				LuaValue luakey = keys[i];
				LuaValue luaval = env.get(luakey);
				String key = luakey.tojstring();
				Object val = toJava(luaval);
				b.put(key, val);
			}
		}

		private Object toJava(LuaValue v) {
			switch (v.type()) {
            case TNIL:
				return null;
            case TSTRING:
				return v.tojstring();
            case TUSERDATA:
				return v.checkuserdata(Object.class);
            case TNUMBER:
				return v.isinttype() ? (Object) new Integer(v.toint()) : (Object) new Double(v.todouble());
			default:
				return v;
			}
		}
	}

	// ------ convert char stream to byte stream for lua compiler -----

	private final class Utf8Encoder extends InputStream {
		private final Reader r;
		private final int[] buf = new int[2];
		private int n;

		private Utf8Encoder(Reader r) {
			this.r = r;
		}

		@Override
		public int read() throws IOException {
			if (n > 0) return buf[--n];
			int c = r.read();
			if (c < 0x80) return c;
			n = 0;
			if (c < 0x800) {
				buf[n++] = (0x80 | (c & 0x3f));
				return (0xC0 | ((c >> 6) & 0x1f));
			} else {
				buf[n++] = (0x80 | (c & 0x3f));
				buf[n++] = (0x80 | ((c >> 6) & 0x3f));
				return (0xE0 | ((c >> 12) & 0x0f));
			}
		}
	}
}
