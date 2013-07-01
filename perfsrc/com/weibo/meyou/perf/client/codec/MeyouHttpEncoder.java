package com.weibo.meyou.perf.client.codec;


import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;


public class MeyouHttpEncoder extends ProtocolEncoderAdapter {
	/**
	 * Creates the instance of the encoder.
	 */
	protected MeyouHttpEncoder() { }
	
	/**
	 * Encodes the protobuf {@link Message} provided into the wire format.
	 * 
	 * @param session The session (not used).
	 * @param message The protobuf {@link Message}.
	 * @param out The encoder output used to write buffer into.
	 */
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		MeyouPerf.MeyouPacket resp = (MeyouPerf.MeyouPacket) message;
		int size = resp.getSerializedSize();

		ByteBuffer buffer = ByteBuffer.allocate(size);			
		if(size > 0) {
			byte[] body = resp.toByteArray();
			buffer.put(body);
		}

		buffer.flip();
		out.write(buffer);
	}
}