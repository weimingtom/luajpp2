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

import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Subclass of {@link LibFunction} which implements the standard lua {@code os} library.
 * <p>
 * It is a usable base with simplified stub functions for library functions that cannot be implemented
 * uniformly on Jse and Jme.
 * <p>
 * This can be installed as-is on either platform, or extended and refined to be used in a complete Jse
 * implementation.
 * <p>
 * Because the nature of the {@code os} library is to encapsulate os-specific features, the behavior of these
 * functions varies considerably from their counterparts in the C platform.
 * <p>
 * The following functions have limited implementations of features that are not supported well on Jme:
 * <ul>
 * <li>{@code execute()}</li>
 * <li>{@code remove()}</li>
 * <li>{@code rename()}</li>
 * <li>{@code tmpname()}</li>
 * </ul>
 * <p>
 * Typically, this library is included as part of a call to either {@code JmePlatform.standardGlobals()}
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
 *     _G.load(new OsLib());
 *     System.out.println(_G.get(&quot;os&quot;).get(&quot;time&quot;).call());
 * }
 * </pre>
 *
 * Doing so will ensure the library is properly initialized and loaded into the globals table.
 * <p>
 *
 * @see LibFunction
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.8">http://www.lua.org/manual/5.1/manual.html#5.8
 *      </a>
 */
@LuaSerializable
public class OsLib extends VarArgFunction {

    private static final long serialVersionUID = 6157286319620735930L;

    public static String TMP_PREFIX = ".luaj";
    public static String TMP_SUFFIX = "tmp";

    private static final int INIT = 0;
    private static final int CLOCK = 1;
    private static final int DATE = 2;
    private static final int DIFFTIME = 3;
    private static final int EXECUTE = 4;
    private static final int EXIT = 5;
    private static final int GETENV = 6;
    private static final int REMOVE = 7;
    private static final int RENAME = 8;
    private static final int SETLOCALE = 9;
    private static final int TIME = 10;
    private static final int TMPNAME = 11;

    private static final String[] NAMES = { "clock", "date", "difftime", "execute", "exit", "getenv",
            "remove", "rename", "setlocale", "time", "tmpname", };

    private static final long t0 = System.currentTimeMillis();
    private static long tmpnames = t0;

    /**
     * Create and OsLib instance.
     */
    public OsLib() {
    }

    public LuaValue init() {
        LuaTable t = new LuaTable();
        bind(t, this.getClass(), NAMES, CLOCK);
        env.set("os", t);
        LuaRunState.getCurrent().setIsLoaded("os", t);
        return t;
    }

    @Override
    public Varargs invoke(Varargs args) {
        try {
            switch (opcode) {
            case INIT:
                return init();
            case CLOCK:
                return valueOf(clock());
            case DATE: {
                String s = args.optjstring(1, null);
                double t = args.optdouble(2, -1);
                return date(s, t == -1 ? System.currentTimeMillis() / 1000.0 : t);
            }
            case DIFFTIME:
                return valueOf(difftime(args.checkdouble(1), args.checkdouble(2)));
            case EXECUTE:
                return valueOf(execute(args.optjstring(1, null)));
            case EXIT:
                exit(args.optint(1, 0));
                return NONE;
            case GETENV: {
                final String val = getenv(args.checkjstring(1));
                return val != null ? valueOf(val) : NIL;
            }
            case REMOVE:
                remove(args.checkjstring(1));
                return TRUE;
            case RENAME:
                rename(args.checkjstring(1), args.checkjstring(2));
                return TRUE;
            case SETLOCALE: {
                String s = setlocale(args.optjstring(1, null), args.optjstring(2, "all"));
                return s != null ? valueOf(s) : NIL;
            }
            case TIME:
                return valueOf(time(args.arg1().isnil() ? null : args.checktable(1)));
            case TMPNAME:
                return valueOf(tmpname());
            }
            return NONE;
        } catch (IOException e) {
            return varargsOf(NIL, valueOf(e.getMessage()));
        }
    }

    /**
     * @return an approximation of the amount in seconds of CPU time used by the program.
     */
    protected double clock() {
        return (System.currentTimeMillis() - t0) / 1000.;
    }

    /**
     * Returns the number of seconds from time t1 to time t2. In POSIX, Windows, and some other systems, this
     * value is exactly t2-t1.
     *
     * @return time difference in seconds
     */
    protected double difftime(double t2, double t1) {
        return t2 - t1;
    }

    /**
     * If the time argument is present, this is the time to be formatted (see the os.time function for a
     * description of this value). Otherwise, date formats the current time.
     *
     * If format starts with '!', then the date is formatted in Coordinated Universal Time. After this
     * optional character, if format is the string "*t", then date returns a table with the following fields:
     * year (four digits), month (1--12), day (1--31), hour (0--23), min (0--59), sec (0--61), wday (weekday,
     * Sunday is 1), yday (day of the year), and isdst (daylight saving flag, a boolean).
     *
     * If format is not "*t", then date returns the date as a string, formatted according to the same rules as
     * the C function strftime.
     *
     * When called without arguments, date returns a reasonable date and time representation that depends on
     * the host system and on the current locale (that is, os.date() is equivalent to os.date("%c")).
     *
     * @param time time since epoch, or -1 if not supplied
     * @return a LString or a LTable containing date and time, formatted according to the given string format.
     */
    protected LuaValue date(String format, double time) {
        Date date = new Date((long)time);

        SimpleDateFormat df;
        if (format == null) {
            df = new SimpleDateFormat();
            return valueOf(df.format(date));
        } else if (format.equals("*t")) {
            Calendar c = Calendar.getInstance();
            LuaTable t = new LuaTable();
            t.rawset("year", valueOf(String.format("%04d", c.get(Calendar.YEAR))));
            t.rawset("month", 1 + c.get(Calendar.MONTH));
            t.rawset("day", c.get(Calendar.DAY_OF_MONTH));
            t.rawset("hour", c.get(Calendar.HOUR_OF_DAY));
            t.rawset("min", c.get(Calendar.MINUTE));
            t.rawset("sec", c.get(Calendar.SECOND));
            t.rawset("wday", c.get(Calendar.DAY_OF_WEEK));
            t.rawset("yday", c.get(Calendar.DAY_OF_YEAR));
            t.rawset("isdst", valueOf(c.get(Calendar.DST_OFFSET) != 0));
            return t;
        } else {
            df = new SimpleDateFormat(format);
            return valueOf(df.format(date));
        }
    }

    /**
     * This function is equivalent to the C function system. It passes command to be executed by an operating
     * system shell. It returns a status code, which is system-dependent. If command is absent, then it
     * returns nonzero if a shell is available and zero otherwise.
     *
     * @param command command to pass to the system
     */
    protected int execute(String command) {
        return 0;
    }

    /**
     * Calls the C function exit, with an optional code, to terminate the host program.
     *
     * @param code Exit code.
     */
    protected void exit(int code) {
        // System.exit(code);
        throw new RuntimeException("Program termination is not allowed");
    }

    /**
     * Returns the value of the process environment variable varname, or null if the variable is not defined.
     *
     * @return String value, or null if not defined
     */
    protected String getenv(String varname) {
        try {
            return System.getProperty(varname);
        } catch (RuntimeException re) {
            return null;
        }
    }

    /**
     * Deletes the file or directory with the given name. Directories must be empty to be removed. If this
     * function fails, it throws and IOException
     *
     * @param filename Name of the file or directory to delete.
     * @throws IOException if it fails
     */
    protected void remove(String filename) throws IOException {
        throw new IOException("not implemented");
    }

    /**
     * Renames file or directory named oldname to newname. If this function fails,it throws and IOException
     *
     * @param oldname old file name
     * @param newname new file name
     * @throws IOException if it fails
     */
    protected void rename(String oldname, String newname) throws IOException {
        throw new IOException("not implemented");
    }

    /**
     * Sets the current locale of the program. locale is a string specifying a locale; category is an optional
     * string describing which category to change: "all", "collate", "ctype", "monetary", "numeric", or
     * "time"; the default category is "all".
     *
     * If locale is the empty string, the current locale is set to an implementation- defined native locale.
     * If locale is the string "C", the current locale is set to the standard C locale.
     *
     * When called with null as the first argument, this function only returns the name of the current locale
     * for the given category.
     *
     * @param locale Locale to set.
     * @param category Category to set the locale for.
     * @return the name of the new locale, or null if the request cannot be honored.
     */
    protected String setlocale(String locale, String category) {
        return "C";
    }

    /**
     * Returns the current time when called without arguments, or a time representing the date and time
     * specified by the given table. This table must have fields year, month, and day, and may have fields
     * hour, min, sec, and isdst (for a description of these fields, see the os.date function).
     *
     * @param table settings
     * @return long value for the time
     */
    protected long time(LuaTable table) {
        return System.currentTimeMillis();
    }

    /**
     * Returns a string with a file name that can be used for a temporary file. The file must be explicitly
     * opened before its use and explicitly removed when no longer needed.
     *
     * On some systems (POSIX), this function also creates a file with that name, to avoid security risks.
     * (Someone else might create the file with wrong permissions in the time between getting the name and
     * creating the file.) You still have to open the file to use it and to remove it (even if you do not use
     * it).
     *
     * @return String filename to use
     */
    protected String tmpname() {
        synchronized (OsLib.class) {
            return TMP_PREFIX + (tmpnames++) + TMP_SUFFIX;
        }
    }
}
