/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
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
package nl.weeaboo.lua2.lib;

import static nl.weeaboo.lua2.vm.LuaConstants.EMPTYSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code table} library.
 * <p>
 * To instantiate and use it directly, link it into your globals table via {@link LuaValue#load(LuaValue)}
 * using code such as:
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaTable _G = new LuaTable();
 *     LuaThread.setGlobals(_G);
 *     _G.load(new BaseLib());
 *     _G.load(new PackageLib());
 *     _G.load(new TableLib());
 *     LuaValue tbl = LuaValue.listOf(new LuaValue[] { LuaValue.valueOf(&quot;abc&quot;), LuaValue.valueOf(&quot;def&quot;) });
 *     LuaValue sep = LuaValue.valueOf(&quot;-&quot;);
 *     System.out.println(_G.get(&quot;table&quot;).get(&quot;concat&quot;).call(tbl, sep));
 * }
 * </pre>
 *
 * Doing so will ensure the library is properly initialized and loaded into the globals table.
 * <p>
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.5">http://www.lua.org/manual/5.1/manual.html#5.5
 *      </a>
 */
@LuaSerializable
public class TableLib extends OneArgFunction {

    private static final long serialVersionUID = 6765625148783166362L;

    public TableLib() {
    }

    private LuaTable init() {
        LuaTable t = new LuaTable();
        bind(t, TableLib.class, new String[] { "getn", "maxn", }, 1);
        bind(t, TableLibV.class,
                new String[] { "remove", "concat", "insert", "sort", "foreach", "foreachi", });
        env.set("table", t);
        LuaRunState.getCurrent().setIsLoaded("table", t);
        return t;
    }

    @Override
    public LuaValue call(LuaValue arg) {
        switch (opcode) {
        case 0: // init library
            return init();
        case 1: // "getn" (table) -> number
            return arg.checktable().getn();
        case 2: // "maxn" (table) -> number
            return valueOf(arg.checktable().maxn());
        }
        return NIL;
    }

    @LuaSerializable
    static final class TableLibV extends VarArgFunction {

        private static final long serialVersionUID = 1811889310685577142L;

        @Override
        public Varargs invoke(Varargs args) {
            switch (opcode) {
            case 0: { // "remove" (table [, pos]) -> removed-ele
                LuaTable table = args.checktable(1);
                int pos = args.narg() > 1 ? args.checkint(2) : 0;
                return table.remove(pos);
            }
            case 1: { // "concat" (table [, sep [, i [, j]]]) -> string
                LuaTable table = args.checktable(1);
                return table.concat(args.optstring(2, EMPTYSTRING), args.optint(3, 1),
                        args.isvalue(4) ? args.checkint(4) : table.length());
            }
            case 2: { // "insert" (table, [pos,] value) -> prev-ele
                final LuaTable table = args.checktable(1);
                final int pos = args.narg() > 2 ? args.checkint(2) : 0;
                final LuaValue value = args.arg(args.narg() > 2 ? 3 : 2);
                table.insert(pos, value);
                return NONE;
            }
            case 3: { // "sort" (table [, comp]) -> void
                LuaTable table = args.checktable(1);
                LuaValue compare = (args.isnoneornil(2) ? NIL : args.checkfunction(2));
                table.sort(compare);
                return NONE;
            }
            case 4: { // (table, func) -> void
                return args.checktable(1).foreach(args.checkfunction(2));
            }
            case 5: { // "foreachi" (table, func) -> void
                return args.checktable(1).foreachi(args.checkfunction(2));
            }
            }
            return NONE;
        }
    }
}
