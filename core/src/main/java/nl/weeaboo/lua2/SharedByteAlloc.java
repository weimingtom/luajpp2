package nl.weeaboo.lua2;

/**
 * <b>Warning: Not thread safe.</b> Use getInstance() for a thread local version.
 */
public final class SharedByteAlloc {

	private static final ThreadLocal<SharedByteAlloc> alloc = new ThreadLocal<SharedByteAlloc>() {
		@Override
		public SharedByteAlloc initialValue() {
			return new SharedByteAlloc();
		}
	};

	private static final int ALLOC_SIZE = 256; //Balance re-use of byte arrays with the overhead of left-over bytes.

	private byte[] current;
	private int offset;

    private SharedByteAlloc() {
    }

	/**
	 * Reserved space in the current byte array, returns the offset of the reserved segment.
	 */
	public int reserve(int len) {
		if (current == null || current.length - offset < len) {
			current = new byte[Math.max(len, ALLOC_SIZE)];
			offset = 0;
		}
		int result = offset;
		offset += len;
		return result;
	}

	public byte[] getReserved() {
		return current;
	}

	public static SharedByteAlloc getInstance() {
		return alloc.get();
	}

}
