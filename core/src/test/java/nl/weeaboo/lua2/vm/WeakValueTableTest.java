package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import org.junit.Assert;

public class WeakValueTableTest extends WeakTableTest {

    @Override
    protected LuaTable newTable() {
        return WeakTable.make(false, true);
    }

    @Override
    protected LuaTable newTable(int narray, int nhash) {
        return WeakTable.make(false, true);
    }

    public void testWeakValuesTable() {
        LuaTable t = newTable();

        Object obj = new Object();
        LuaTable tableValue = new LuaTable();
        LuaString stringValue = LuaString.valueOf("this is a test");
        LuaTable tableValue2 = new LuaTable();

        t.set("table", tableValue);
        t.set("userdata", LuaValue.userdataOf(obj, null));
        t.set("string", stringValue);
        t.set("string2", LuaValue.valueOf("another string"));
        t.set(1, tableValue2);
        Assert.assertTrue("table must have at least 4 elements", t.getHashLength() >= 4);
        Assert.assertTrue("array part must have 1 element", t.getArrayLength() >= 1);

        // check that table can be used to get elements
        Assert.assertEquals(tableValue, t.get("table"));
        Assert.assertEquals(stringValue, t.get("string"));
        Assert.assertEquals(obj, t.get("userdata").checkuserdata());
        Assert.assertEquals(tableValue2, t.get(1));

        // nothing should be collected, since we have strong references here
        collectGarbage();

        // check that elements are still there
        Assert.assertEquals(tableValue, t.get("table"));
        Assert.assertEquals(stringValue, t.get("string"));
        Assert.assertEquals(obj, t.get("userdata").checkuserdata());
        Assert.assertEquals(tableValue2, t.get(1));

        // drop our strong references
        obj = null;
        tableValue = null;
        tableValue2 = null;
        stringValue = null;

        // Garbage collection should cause weak entries to be dropped.
        collectGarbage();

        // check that they are dropped
        Assert.assertEquals(NIL, t.get("table"));
        Assert.assertEquals(NIL, t.get("userdata"));
        Assert.assertEquals(NIL, t.get(1));
        Assert.assertFalse("strings should not be in weak references", t.get("string").isnil());
    }
}