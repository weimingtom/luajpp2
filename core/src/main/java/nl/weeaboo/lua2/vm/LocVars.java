/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
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
package nl.weeaboo.lua2.vm;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * Data class to hold debug information relatign to local variables for a
 * {@link Prototype}
 */
@LuaSerializable
public final class LocVars implements Externalizable {

	// --- Uses manual serialization, don't add variables ---
	/** The local variable name */
	public LuaString varname;

	/** The instruction offset when the variable comes into scope */
	public int startpc;

	/** The instruction offset when the variable goes out of scope */
	public int endpc;
	// --- Uses manual serialization, don't add variables ---

	public LocVars() {		
	}
	
	/**
	 * Construct a LocVars instance.
	 * 
	 * @param varname The local variable name
	 * @param startpc The instruction offset when the variable comes into scope
	 * @param endpc The instruction offset when the variable goes out of scope
	 */
	public LocVars(LuaString varname, int startpc, int endpc) {
		this.varname = varname;
		this.startpc = startpc;
		this.endpc = endpc;
	}

	public String tojstring() {
		return varname + " " + startpc + "-" + endpc;
	}
	
	@Override
	public String toString() {
		return tojstring();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		int len = varname.length();
		out.writeInt(len);
		varname.write(out, 0, len);
		
		out.writeInt(startpc);
		out.writeInt(endpc);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int len = in.readInt();
		byte[] bytes = new byte[len];
		in.readFully(bytes);
		varname = LuaString.valueOf(bytes);
		
		startpc = in.readInt();
		endpc = in.readInt();
	}
	
}
