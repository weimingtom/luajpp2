package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.LuaThreadGroup;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib2.LuaBoundFunction;
import nl.weeaboo.lua2.link.ILuaLink;
import nl.weeaboo.lua2.link.LuaLink;
import nl.weeaboo.lua2.luajava.LuajavaLib;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class ThreadLib extends LuaModule {

    private static final long serialVersionUID = 1L;

    public ThreadLib() {
        super("Thread");
    }

    /**
     * Creates a new thread in the default thread group.
     *
     * @param args
     *        <ol>
     *        <li>Lua closure to call in a new thread
     *        <li>Vararg of arguments to pass to the closure
     *        </ol>
     */
    @LuaBoundFunction(luaName = "new")
    public Varargs new_(Varargs args) {
        LuaRunState lrs = LuaRunState.getCurrent();
        LuaClosure func = args.arg1().checkclosure();
        LuaLink result = lrs.newThread(func, args.subargs(2));
        return LuajavaLib.toUserdata(result, result.getClass());
    }

    /**
     * Creates a new thread group.
     *
     * @param args (not used)
     */
    @LuaBoundFunction
    public Varargs newGroup(Varargs args) {
        LuaRunState lrs = LuaRunState.getCurrent();
        LuaThreadGroup result = lrs.newThreadGroup();
        return LuajavaLib.toUserdata(result, result.getClass());
    }

    /**
     * Stops executing the current thread.
     *
     * @param args
     *        <ol>
     *        <li>Number of frames to yield. If negative, the wait is infinite.
     *        </ol>
     */
    @LuaBoundFunction
    public Varargs yield(Varargs args) {
        LuaRunState lrs = LuaRunState.getCurrent();
        ILuaLink link = lrs.getCurrentLink();

        if (link != null && !args.isnil(1)) {
            int w = args.toint(1);
            link.setWait(w <= 0 ? w : w - 1);
        }

        final LuaThread running = LuaThread.getRunning();
        return running.yield(args);
    }

    /**
     * Ends the current call stack frame, then yields the current thread. This is effectively a return,
     * immediately followed by a {@link #yield(Varargs)}.
     *
     * @param args
     *        <ol>
     *        <li>Number of frames to yield. If negative, the wait is infinite.
     *        </ol>
     */
    @LuaBoundFunction
    public Varargs endCall(Varargs args) {
        LuaRunState lrs = LuaRunState.getCurrent();
        ILuaLink link = lrs.getCurrentLink();

        if (link != null && !args.isnil(1)) {
            int w = args.toint(1);
            link.setWait(w <= 0 ? w : w - 1);
        }

        final LuaThread running = LuaThread.getRunning();
        return running.endCall(args);
    }

    /**
     * Goto a different script file
     *
     * @param args
     *        <ol>
     *        <li>Filename of the script to jump to.
     *        </ol>
     */
    @LuaBoundFunction
    public Varargs jump(Varargs args) {
        Varargs v = BaseLib.loadFile(args.checkjstring(1));
        if (v.isnil(1)) {
            throw new LuaError(v.tojstring(2));
        }

        // We can only jump to closures
        LuaClosure closure = v.checkclosure(1);

        LuaRunState lrs = LuaRunState.getCurrent();
        ILuaLink link = lrs.getCurrentLink();
        link.jump(closure, NONE);
        return NONE;
    }

}