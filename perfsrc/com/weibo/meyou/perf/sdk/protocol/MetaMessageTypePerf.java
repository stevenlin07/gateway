package com.weibo.meyou.perf.sdk.protocol;

/**
 * @author Eric Liang
 */
public enum MetaMessageTypePerf {
	text((byte) 0x01), 
	audio((byte) 0x02), 
	video((byte) 0x03), 
	image((byte) 0x04), 
	file((byte) 0x05),
	operation((byte) 0x07),
	location((byte) 0x08),
	property((byte) 0x09),
	unknown((byte) 0x0);

	private final byte code;

	private MetaMessageTypePerf(byte code) {
		this.code = code;
	}

	public byte toByte() {
		return code;
	}

	public static MetaMessageTypePerf valueOf(final byte code) {
		for (MetaMessageTypePerf t : MetaMessageTypePerf.values()) {
			if (code == t.code)
				return t;
		}
		return unknown;
	}
}
